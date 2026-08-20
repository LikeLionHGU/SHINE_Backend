package com.shine.backend.domain.compat.service;

import com.shine.backend.domain.compat.dto.ParsedTestItemDto;
import com.shine.backend.domain.compat.dto.ReportResponse;
import com.shine.backend.domain.compat.dto.ReportUploadRequest;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionSource;
import com.shine.backend.domain.question.entity.QuestionStatus;
import com.shine.backend.domain.nutrition.AllowedFoods;
import com.shine.backend.domain.question.repository.QuestionRepository;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.analyzer.AnalyzedRow;
import com.shine.backend.domain.testsheet.analyzer.TestSheetAnalyzer;
import com.shine.backend.domain.testsheet.analyzer.VerdictGenerator;
import com.shine.backend.domain.testsheet.entity.*;
import com.shine.backend.domain.testsheet.ocr.OcrResult;
import com.shine.backend.domain.testsheet.ocr.OcrRow;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 프론트가 읽어낸 검사지를 받아 저장한다.
 *
 * ── 판정 주체 (전달사항 4번) ──────────────────────────────────────────────
 * 프론트에 학회 기준표를 lookup하는 판정 엔진(src/lib/labs)이 들어오면서 역할이 갈렸다.
 *
 *   엔진이 판정한 항목(engineStatus 있음) → 프론트 판정을 그대로 쓴다.
 *       서버 판정에는 근거(출처·인용문)가 붙지 않고, 임신 주수별 기준 반영도
 *       항목마다 들쭉날쭉하다. 화면은 이미 엔진 판정을 쓰고 있으므로 서버가 여기서
 *       다시 덮어쓰면 저장된 값과 화면이 갈린다.
 *   엔진이 모르는 항목                    → 서버 판정을 쓴다(기존 그대로).
 *
 * 어느 쪽을 쓰든 화면에 보이는 것과 DB에 남는 것은 항상 같은 값이어야 한다.
 * 그래야 기록 탭에서 지난 검사지를 열었을 때 상태가 바뀌지 않는다.
 *
 * ── 응답 순서 (전달사항 3번) ─────────────────────────────────────────────
 * 프론트는 응답 items를 배열 인덱스로 요청 items와 짝짓는다. 개수가 하나라도 다르면
 * 응답을 통째로 버리고, 순서가 밀리면 다른 항목의 이름이 붙는다. 그래서 이 서비스는
 * 정렬·중복 제거·미지원 항목 제외를 일절 하지 않고 **받은 순서·개수 그대로** 돌려준다.
 * (인덱스 대신 쓸 수 있도록 응답 항목마다 resultId 를 함께 내려준다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompatReportService {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("yy.MM.dd");

    private final UserRepository userRepository;
    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;
    private final QuestionRepository questionRepository;
    private final TestSheetAnalyzer analyzer;
    private final ValueSplitter valueSplitter;
    private final VerdictGenerator verdictGenerator;
    private final AllowedFoods allowedFoods;
    private final EngineMetaCodec engineMetaCodec;

    @Transactional
    public ReportResponse save(Long userId, ReportUploadRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate testDate = parseDate(request.testDate());
        boolean confirmed = testDate != null;
        LocalDate effective = confirmed ? testDate : LocalDate.now();

        // 검사는 미래에 받을 수 없다. 오늘 이후 날짜는 OCR 오독으로 본다.
        if (effective.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.TEST_DATE_IN_FUTURE);
        }

        // 같은 날짜를 다시 올리면 덮어쓴다.
        // 기록 탭이 날짜 타임라인이라 같은 날 두 줄이 뜨면 사용자가 구분할 수 없다.
        TestSheet sheet = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusAndTestDateOrderByIdDesc(
                        userId, AnalysisStatus.DONE, effective)
                .orElse(null);

        if (sheet == null) {
            sheet = TestSheet.builder()
                    .user(user)
                    .testDate(effective)
                    .testDateConfirmed(confirmed)
                    .pregnancyWeek(user.getPregnancyWeek(effective))
                    .imageKeys(List.of())   // 이미지는 프론트가 기기에 두고 있다
                    .analysisStatus(AnalysisStatus.WAITING)
                    .piiMasked(true)
                    .build();
            testSheetRepository.save(sheet);
        } else {
            testResultRepository.deleteAll(testResultRepository.findByTestSheetId(sheet.getId()));
            questionRepository.deleteAll(
                    questionRepository.findByTestSheetIdOrderByIdDesc(sheet.getId()));
            testResultRepository.flush();
            questionRepository.flush();
        }

        // 요청 순서를 여기서 고정한다. 아래 어디서도 이 리스트를 다시 정렬하지 않는다.
        List<ParsedTestItemDto> items = request.items();

        // 기존 파서·매처·판정 엔진을 그대로 태운다. rows 는 items 와 1:1 이다.
        List<AnalyzedRow> rows = analyzer.analyze(toOcrResult(items));

        final TestSheet target = sheet;
        // 저장된 엔티티를 요청 인덱스로 되찾을 수 있게 자리를 맞춰 담는다.
        // (이름이 비어 있는 항목은 저장하지 않으므로 그 자리는 null 로 남는다)
        TestResult[] saved = new TestResult[items.size()];
        List<TestResult> toPersist = new ArrayList<>();
        List<Integer> persistedIndexes = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            ParsedTestItemDto origin = items.get(i);
            if (origin == null || isBlank(origin.name())) continue;

            AnalyzedRow row = resolveRow(rows.get(i), origin);
            toPersist.add(toEntity(target, row, origin));
            persistedIndexes.add(i);
        }

        List<TestResult> persisted = testResultRepository.saveAll(toPersist);
        for (int i = 0; i < persisted.size(); i++) {
            saved[persistedIndexes.get(i)] = persisted.get(i);
        }

        sheet.markDone(request.summary(), null, "frontend-openai", null);
        // 홈 화면과 검사지 화면에 다른 재료가 뜨지 않도록 프론트가 만든 것을 저장해둔다
        List<ReportUploadRequest.FoodDto> foods = filterFoods(request.foods());
        sheet.applyNutritionFoods(engineMetaCodec.toJson(foods));
        saveQuestions(user, sheet, request.questions());

        long matched = rows.stream().filter(AnalyzedRow::isMatched).count();
        long engineOwned = items.stream().filter(it -> it != null && it.hasEngineVerdict()).count();
        log.info("프론트 검사지 수신 sheetId={} 항목={} 매칭={} 엔진판정={}",
                sheet.getId(), items.size(), matched, engineOwned);

        return new ReportResponse(
                sheet.getId(),
                sheet.getTestDate().format(SHORT),
                sheet.isTestDateConfirmed(),
                weekLabel(sheet.getPregnancyWeek()),
                toItems(items, rows, saved),
                request.summary(),
                request.questions(),
                foods);
    }

    // ---------- 판정 ----------

    /**
     * 저장할 판정을 정한다.
     * 엔진이 판정한 항목은 엔진 값을, 아니면 서버가 계산한 값을 쓴다(전달사항 4번).
     */
    private AnalyzedRow resolveRow(AnalyzedRow row, ParsedTestItemDto origin) {
        if (!origin.hasEngineVerdict()) return row;

        ResultStatus fromEngine = ResultStatus.fromEngineStatus(origin.engineStatus());
        if (fromEngine == null) {
            log.warn("모르는 engineStatus '{}' — 서버 판정을 쓴다", origin.engineStatus());
            return row;
        }
        return row.withResultStatus(fromEngine);
    }

    // ---------- 변환 ----------

    /**
     * 요청 items 를 그대로 OCR 행으로 옮긴다.
     *
     * ⚠️ 여기서 한 줄이라도 건너뛰면 뒤 항목의 인덱스가 앞으로 당겨져서, 응답에 다른
     *    항목의 이름이 붙는다. 이름이 비어 있어 저장할 수 없는 행도 자리는 남긴다.
     */
    private OcrResult toOcrResult(List<ParsedTestItemDto> items) {
        List<OcrRow> rows = new ArrayList<>(items.size());

        for (ParsedTestItemDto item : items) {
            if (item == null) {
                rows.add(OcrRow.of(null, "", null, null, null, null));
                continue;
            }
            var split = valueSplitter.split(item.value());
            rows.add(OcrRow.of(null, item.name() == null ? "" : item.name(),
                    split.value(), split.unit(), split.referenceRange(), null));
        }
        return new OcrResult(null, null, rows, null, "frontend-openai");
    }

    private TestResult toEntity(TestSheet sheet, AnalyzedRow row, ParsedTestItemDto origin) {
        return TestResult.builder()
                .testSheet(sheet)
                .testItem(row.item())
                .user(sheet.getUser())
                .testDate(sheet.getTestDate())
                .pregnancyWeek(sheet.getPregnancyWeek())
                .ocrLabel(trimTo(row.ocrLabel(), 100))
                .ocrCategory(row.ocrCategory())
                .rawValue(row.rawValue() == null ? "" : trimTo(row.rawValue(), 100))
                .resultType(row.resultType())
                .numberValue(row.numberValue())
                .textValue(trimTo(row.textValue(), 100))
                .unit(row.unit())
                .unitRaw(row.unitRaw())
                .sheetNormalMin(row.sheetNormalMin())
                .sheetNormalMax(row.sheetNormalMax())
                .sheetNormalText(row.sheetNormalText())
                .normalRangeSource(row.normalRangeSource())
                .resultStatus(row.resultStatus())
                .sheetVerdict(row.sheetVerdict())
                .verdictMismatch(row.verdictMismatch())
                // 저장해둬야 나중에 /test-sheets/{id} 로 다시 볼 때도 같은 문장이 나온다
                .briefForMom(trimTo(briefFor(row, origin), 500))
                .engineStatus(trimTo(origin.engineStatus(), 20))
                .engineMeta(engineMetaCodec.toJson(EngineMetaCodec.EngineMeta.from(origin)))
                .editedByUser(false)
                .build();
    }

    /** 화면에 뜬 문장을 그대로 남긴다. 엔진 판정이면 엔진 문장이 곧 화면 문장이다. */
    private String briefFor(AnalyzedRow row, ParsedTestItemDto origin) {
        if (origin.hasEngineVerdict() && !isBlank(origin.verdict())) {
            return origin.verdict();
        }
        return verdictGenerator.generate(row);
    }

    /**
     * 응답 items 를 만든다. 요청과 **같은 개수·같은 순서**다.
     * 정렬하거나 걸러내면 프론트에서 항목 이름이 뒤섞인다(전달사항 3번).
     */
    private List<ParsedTestItemDto> toItems(List<ParsedTestItemDto> items,
                                            List<AnalyzedRow> rows,
                                            TestResult[] saved) {
        List<ParsedTestItemDto> result = new ArrayList<>(items.size());

        for (int i = 0; i < items.size(); i++) {
            ParsedTestItemDto origin = items.get(i);
            if (origin == null) {
                result.add(null);   // 자리를 비워서라도 개수를 지킨다
                continue;
            }

            AnalyzedRow row = rows.get(i);
            TestItemCatalog item = row.item();
            String catalogName = item == null ? null : item.getNameKo();
            String itemCode = item == null ? null : item.getCode();
            Long resultId = saved[i] == null ? null : saved[i].getId();

            if (origin.hasEngineVerdict()) {
                // 판정·설명·근거는 엔진 것을 유지하고 대표명만 빌려준다
                result.add(origin.withCatalogName(catalogName, resultId, itemCode));
                continue;
            }

            result.add(origin.withServerVerdict(
                    catalogName,
                    row.resultStatus().label(),
                    item != null && item.getBriefForMom() != null
                            ? item.getBriefForMom() : origin.definition(),
                    verdict(row, origin),
                    resultId,
                    itemCode));
        }
        return result;
    }

    /**
     * 판정을 못 했으면 못 했다고 말한다.
     *
     * 프론트가 보낸 문장을 그대로 돌려주면 저장된 값과 어긋나서,
     * 업로드 직후엔 "안심"이던 항목이 기록 탭에서는 확인 필요로 바뀐다.
     * 사용자 눈에는 데이터가 망가진 것으로 보인다.
     */
    private String verdict(AnalyzedRow row, ParsedTestItemDto origin) {
        if (row.resultStatus() == ResultStatus.UNKNOWN) return null;
        String generated = verdictGenerator.generate(row);
        return generated != null ? generated : origin.verdict();
    }

    private void saveQuestions(User user, TestSheet sheet, List<String> questions) {
        if (questions == null || questions.isEmpty()) return;

        questionRepository.saveAll(questions.stream()
                .filter(q -> q != null && !q.isBlank())
                .map(q -> Question.builder()
                        .user(user)
                        .testSheet(sheet)
                        .content(trimTo(q, 500))
                        // AI가 "물어볼 질문"을 추천한 것이지 답변이 아니다
                        .createdBy(QuestionSource.AI)
                        .questionStatus(QuestionStatus.PENDING)
                        .includeInBriefing(true)
                        .build())
                .toList());
    }

    /**
     * 임신 시작 이전 검사지는 주차를 말할 수 없다.
     * 임신 전 건강검진 결과를 올리는 경우가 실제로 있어서 거부하지는 않고,
     * 주차만 비워 보낸다. "-335주차"가 화면에 뜨는 것보다 낫다.
     */
    private String weekLabel(int week) {
        return week < 0 ? null : week + "주차";
    }

    /** "26.08.17" 과 "2026-08-17" 을 모두 받는다 */
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        try {
            if (value.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
                return LocalDate.parse(value, SHORT);
            }
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("검사일 파싱 실패 '{}'", raw);
            return null;
        }
    }

    /**
     * 목록에 없는 음식을 걸러낸다.
     * 프롬프트로 제약해도 AI가 가끔 벗어나고, 이미지가 없는 재료가 화면에 뜨면 깨진다.
     * 임신 중 금기 식품(동물의 간, 참치 대뱃살, 다시마)이 섞이는 것도 여기서 막힌다.
     */
    private List<ReportUploadRequest.FoodDto> filterFoods(List<ReportUploadRequest.FoodDto> foods) {
        if (foods == null || foods.isEmpty()) return List.of();

        LinkedHashMap<String, ReportUploadRequest.FoodDto> picked = new LinkedHashMap<>();
        for (ReportUploadRequest.FoodDto food : foods) {
            String canonical = allowedFoods.canonicalize(food.name());
            if (canonical == null) {
                log.info("추천 목록 밖 음식 제외: {}", food.name());
                continue;
            }
            picked.putIfAbsent(canonical,
                    new ReportUploadRequest.FoodDto(canonical, food.reason()));
        }
        return List.copyOf(picked.values());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** DB 컬럼 길이를 넘기면 저장 자체가 실패해 검사지 한 장이 통째로 날아간다. */
    private static String trimTo(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

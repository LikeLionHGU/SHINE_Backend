package com.shine.backend.domain.compat.service;

import com.shine.backend.domain.compat.dto.ParsedTestItemDto;
import com.shine.backend.domain.compat.dto.ReportResponse;
import com.shine.backend.domain.compat.dto.ReportUploadRequest;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionSource;
import com.shine.backend.domain.question.entity.QuestionStatus;
import com.shine.backend.domain.nutrition.AllowedFoods;
import com.shine.backend.domain.question.repository.QuestionRepository;
import com.shine.backend.domain.testitem.entity.ResultType;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 프론트가 OpenAI Vision으로 읽어낸 검사지를 받아 저장한다.
 *
 * 프론트가 보낸 status는 참고만 하고, 화면에 쓸 판정은 서버가 다시 계산한다.
 * 검사지에 인쇄된 참고치가 비임신 성인 기준인 경우가 많아 그대로 믿으면
 * 정상인 산모가 "주의"로 나오기 때문이다.
 *   혈색소 11.5 → 검사지 기준(12~16) 주의 / 임신 기준(11~15) 안심
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
    private final ObjectMapper objectMapper;

    @Transactional
    public ReportResponse save(Long userId, ReportUploadRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate testDate = parseDate(request.testDate());
        boolean confirmed = testDate != null;
        LocalDate effective = confirmed ? testDate : LocalDate.now();

        TestSheet sheet = TestSheet.builder()
                .user(user)
                .testDate(effective)
                .testDateConfirmed(confirmed)
                .pregnancyWeek(user.getPregnancyWeek(effective))
                .imageKeys(List.of())   // 이미지는 프론트가 기기에 두고 있다
                .analysisStatus(AnalysisStatus.WAITING)
                .piiMasked(true)
                .build();
        testSheetRepository.save(sheet);

        // 기존 파서·매처·판정 엔진을 그대로 태운다
        List<AnalyzedRow> rows = analyzer.analyze(toOcrResult(request));
        testResultRepository.saveAll(rows.stream().map(row -> toEntity(sheet, row)).toList());

        sheet.markDone(request.summary(), null, "frontend-openai", null);
        // 홈 화면과 검사지 화면에 다른 재료가 뜨지 않도록 프론트가 만든 것을 저장해둔다
        List<ReportUploadRequest.FoodDto> foods = filterFoods(request.foods());
        sheet.applyNutritionFoods(toJson(foods));
        saveQuestions(user, sheet, request.questions());

        long matched = rows.stream().filter(AnalyzedRow::isMatched).count();
        log.info("프론트 검사지 수신 sheetId={} 항목={} 매칭={}", sheet.getId(), rows.size(), matched);

        return new ReportResponse(
                sheet.getId(),
                sheet.getTestDate().format(SHORT),
                sheet.isTestDateConfirmed(),
                sheet.getPregnancyWeek() + "주차",
                toItems(rows, request.items()),
                request.summary(),
                request.questions(),
                foods);
    }

    // ---------- 변환 ----------

    private OcrResult toOcrResult(ReportUploadRequest request) {
        List<OcrRow> rows = new ArrayList<>();

        for (ParsedTestItemDto item : request.items()) {
            if (item.name() == null || item.name().isBlank()) continue;

            var split = valueSplitter.split(item.value());
            rows.add(OcrRow.of(null, item.name(), split.value(), split.unit(),
                    split.referenceRange(), null));
        }
        return new OcrResult(null, null, rows, null, "frontend-openai");
    }

    private TestResult toEntity(TestSheet sheet, AnalyzedRow row) {
        return TestResult.builder()
                .testSheet(sheet)
                .testItem(row.item())
                .user(sheet.getUser())
                .testDate(sheet.getTestDate())
                .pregnancyWeek(sheet.getPregnancyWeek())
                .ocrLabel(row.ocrLabel())
                .ocrCategory(row.ocrCategory())
                .rawValue(row.rawValue() == null ? "" : row.rawValue())
                .resultType(row.resultType())
                .numberValue(row.numberValue())
                .textValue(row.textValue())
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
                .briefForMom(verdictGenerator.generate(row))
                .editedByUser(false)
                .build();
    }

    /** 서버 판정으로 status를 덮어쓰고, 설명 문장도 템플릿으로 다시 만든다 */
    private List<ParsedTestItemDto> toItems(List<AnalyzedRow> rows, List<ParsedTestItemDto> original) {
        List<ParsedTestItemDto> result = new ArrayList<>();

        for (int i = 0; i < rows.size() && i < original.size(); i++) {
            AnalyzedRow row = rows.get(i);
            ParsedTestItemDto origin = original.get(i);
            TestItemCatalog item = row.item();

            result.add(new ParsedTestItemDto(
                    item != null ? item.getNameKo() : origin.name(),
                    origin.value(),
                    statusLabel(row.resultStatus(), origin.status()),
                    item != null && item.getBriefForMom() != null
                            ? item.getBriefForMom() : origin.definition(),
                    verdict(row, item, origin)));
        }
        return result;
    }

    /**
     * 판정을 못 했으면 못 했다고 말한다.
     *
     * 프론트가 보낸 값을 그대로 돌려주면 저장된 값과 어긋나서,
     * 업로드 직후엔 "안심"이던 항목이 기록 탭에서는 미분류로 바뀐다.
     * 사용자 눈에는 데이터가 망가진 것으로 보인다.
     */
    private String statusLabel(ResultStatus status, String fallback) {
        return switch (status) {
            case NORMAL -> "안심";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> null;
        };
    }

    /**
     * 첫 문장은 고정 템플릿으로 만든다.
     * AI가 만든 문장을 그대로 쓰면 서버 판정과 어긋날 수 있다.
     */
    private String verdict(AnalyzedRow row, TestItemCatalog item, ParsedTestItemDto origin) {
        // 판정을 못 한 항목에 AI가 쓴 "정상입니다"를 붙이면 저장된 값과 어긋난다
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
                        .content(q.length() > 500 ? q.substring(0, 500) : q)
                        // AI가 "물어볼 질문"을 추천한 것이지 답변이 아니다
                        .createdBy(QuestionSource.AI)
                        .questionStatus(QuestionStatus.PENDING)
                        .includeInBriefing(true)
                        .build())
                .toList());
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

    private String toJson(List<ReportUploadRequest.FoodDto> foods) {
        if (foods == null || foods.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(foods);
        } catch (Exception e) {
            log.warn("추천 음식 직렬화 실패", e);
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
}

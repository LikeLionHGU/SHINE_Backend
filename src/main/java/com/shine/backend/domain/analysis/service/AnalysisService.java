package com.shine.backend.domain.analysis.service;

import com.shine.backend.domain.analysis.dto.*;
import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("M월 d일");
    private static final int SPARKLINE_SIZE = 6;

    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;

    @Transactional(readOnly = true)
    public AnalysisListResponse getItems(Long userId, LocalDate date, String keyword, ResultStatus status) {
        TestSheet sheet = resolveSheet(userId, date);
        if (sheet == null) return AnalysisListResponse.empty();

        List<TestResult> results = testResultRepository.findBySheetWithItem(sheet.getId());

        // 스파크라인용 추이를 한 번에 가져온다. 항목마다 조회하면 N+1이다.
        List<Long> itemIds = results.stream()
                .map(TestResult::getTestItem).filter(Objects::nonNull)
                .map(TestItemCatalog::getId).distinct().toList();

        Map<Long, List<TestResult>> trendMap = itemIds.isEmpty() ? Map.of()
                : testResultRepository.findTrendsByItemIds(userId, itemIds).stream()
                .collect(Collectors.groupingBy(r -> r.getTestItem().getId()));

        List<AnalysisItemResponse> items = results.stream()
                .filter(r -> matchesKeyword(r, keyword))
                .filter(r -> status == null || r.getResultStatus() == status)
                .sorted(Comparator
                        .comparingInt((TestResult r) -> severity(r.getResultStatus()))
                        .thenComparing(r -> r.getTestItem() == null ? 999 : r.getTestItem().getDisplayOrder()))
                .map(r -> toItem(r, trendMap))
                .toList();

        return new AnalysisListResponse(
                sheet.getId(),
                sheet.getTestDate(),
                sheet.getTestDate().format(FMT),
                sheet.getPregnancyWeek(),
                testSheetRepository.findFirstByUserIdAndTestDateLessThanOrderByTestDateDesc(
                        userId, sheet.getTestDate()).map(TestSheet::getTestDate).orElse(null),
                testSheetRepository.findFirstByUserIdAndTestDateGreaterThanOrderByTestDateAsc(
                        userId, sheet.getTestDate()).map(TestSheet::getTestDate).orElse(null),
                items);
    }

    @Transactional(readOnly = true)
    public AnalysisTrendResponse getTrend(Long userId, Long testItemId) {
        List<TestResult> trend = testResultRepository.findTrend(userId, testItemId);
        if (trend.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_TREND_DATA);
        }

        TestResult latest = trend.get(trend.size() - 1);
        TestItemCatalog item = latest.getTestItem();

        List<AnalysisTrendResponse.TrendPoint> points = trend.stream()
                .map(r -> new AnalysisTrendResponse.TrendPoint(
                        r.getTestSheet().getId(),
                        r.getTestDate(),
                        r.getTestDate().format(FMT),
                        r.getPregnancyWeek(),
                        r.getNumberValue(),
                        r.getTextValue(),
                        r.getResultStatus()))
                .toList();

        return new AnalysisTrendResponse(
                item.getId(),
                item.getNameKo(),
                item.getNameEn(),
                item.getUnit(),
                item.getResultType(),
                item.getBriefForMom(),
                new AnalysisTrendResponse.Latest(
                        latest.getNumberValue(), latest.getTextValue(),
                        latest.getResultStatus(), label(latest.getResultStatus()),
                        latest.getTestDate(), latest.getPregnancyWeek()),
                new AnalysisTrendResponse.NormalRange(
                        item.getNormalMin(), item.getNormalMax(), latest.getNormalRangeSource()),
                points,
                describeTrend(item, trend));
    }

    // ---------- 내부 ----------

    private TestSheet resolveSheet(Long userId, LocalDate date) {
        if (date != null) {
            return testSheetRepository.findFirstByUserIdAndTestDateOrderByIdDesc(userId, date).orElse(null);
        }
        return testSheetRepository.findFirstByUserIdAndAnalysisStatusOrderByTestDateDesc(
                userId, com.shine.backend.domain.testsheet.entity.AnalysisStatus.DONE).orElse(null);
    }

    private AnalysisItemResponse toItem(TestResult r, Map<Long, List<TestResult>> trendMap) {
        TestItemCatalog item = r.getTestItem();
        List<TestResult> history = item == null ? List.of()
                : trendMap.getOrDefault(item.getId(), List.of());

        return new AnalysisItemResponse(
                item == null ? null : item.getId(),
                r.getId(),
                item == null ? r.getOcrLabel() : item.getNameKo(),
                r.getResultStatus(),
                label(r.getResultStatus()),
                r.getNumberValue(),
                r.getTextValue(),
                r.getUnit(),
                sparkline(item, history),
                history.size() >= 2);
    }

    /**
     * 정성 항목은 꺾은선을 그릴 수 없다. 음성/양성에는 높낮이가 없다.
     * 측정이 1회뿐이어도 선이 안 나오므로 null을 준다.
     */
    private List<BigDecimal> sparkline(TestItemCatalog item, List<TestResult> history) {
        if (item == null || !item.isTrendable() || item.getResultType() != ResultType.NUMBER) return null;

        List<BigDecimal> values = history.stream()
                .map(TestResult::getNumberValue)
                .filter(Objects::nonNull)
                .toList();

        if (values.size() < 2) return null;
        return values.size() <= SPARKLINE_SIZE
                ? values
                : values.subList(values.size() - SPARKLINE_SIZE, values.size());
    }

    /**
     * 추이 설명. 지금은 코드가 만든다.
     *
     * 나중에 AI 문장을 붙이더라도 이 문장을 먼저 만들어두고 그 위에 얹는다.
     * 반대로 하면 AI 장애 시 화면이 빈다.
     * 원인 단정이나 진단명은 넣지 않고, 항상 의사 확인을 권하는 문장으로 끝낸다.
     */
    private String describeTrend(TestItemCatalog item, List<TestResult> trend) {
        String base = item.getBriefForMom() == null ? "" : item.getBriefForMom() + "\n";

        if (trend.size() < 2) {
            return base + "아직 비교할 이전 검사가 없어요. 다음 검사 후에 변화를 보여드릴게요.";
        }

        BigDecimal first = trend.get(0).getNumberValue();
        BigDecimal last = trend.get(trend.size() - 1).getNumberValue();

        if (first == null || last == null) {
            return base + "최근 %d번의 검사 결과를 확인할 수 있어요.".formatted(trend.size());
        }

        String direction = switch (last.compareTo(first)) {
            case 1 -> "수치가 올라가는 모습이 보여요";
            case -1 -> "수치가 내려가는 모습이 보여요";
            default -> "수치가 비슷하게 유지되고 있어요";
        };

        return base
                + "최근 %d번의 검사에서 %s.\n".formatted(trend.size(), direction)
                + "자연스러운 변화일 수도 있지만, 변화 폭이 괜찮은지\n의사 선생님과 함께 확인해보세요.";
    }

    private boolean matchesKeyword(TestResult r, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String k = keyword.trim().toLowerCase();

        if (r.getOcrLabel() != null && r.getOcrLabel().toLowerCase().contains(k)) return true;
        TestItemCatalog item = r.getTestItem();
        if (item == null) return false;

        return (item.getNameKo() != null && item.getNameKo().toLowerCase().contains(k))
                || (item.getNameEn() != null && item.getNameEn().toLowerCase().contains(k));
    }

    private int severity(ResultStatus status) {
        return switch (status) {
            case DANGER -> 0;
            case CAUTION -> 1;
            case NORMAL -> 2;
            case UNKNOWN -> 3;
        };
    }

    private String label(ResultStatus status) {
        return switch (status) {
            case NORMAL -> "안심";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> null;
        };
    }
}

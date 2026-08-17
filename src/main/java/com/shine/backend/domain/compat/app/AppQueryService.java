package com.shine.backend.domain.compat.app;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppQueryService {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("yy.MM.dd");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M월 d일");
    private static final List<String> ZONE_LABELS = List.of("높음", "안정", "낮음");
    private static final int RECORD_SIZE = 50;

    private final UserRepository userRepository;
    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;

    // ---------- 마이 ----------

    @Transactional(readOnly = true)
    public AppDtos.UserProfile getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new AppDtos.UserProfile(
                user.getName(),
                user.getNickname() == null ? user.getLoginId() : user.getNickname(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getGuardianEmail(),
                user.getAdditionalEmail());
    }

    // ---------- 기록 탭 ----------

    @Transactional(readOnly = true)
    public List<AppDtos.RecordEntry> getRecords(Long userId) {
        List<TestSheet> sheets = testSheetRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, RECORD_SIZE));

        if (sheets.isEmpty()) return List.of();

        Map<Long, Map<ResultStatus, Long>> counts = countByStatus(
                sheets.stream().map(TestSheet::getId).toList());

        return sheets.stream()
                .filter(s -> s.getAnalysisStatus() == AnalysisStatus.DONE)
                .map(s -> new AppDtos.RecordEntry(
                        String.valueOf(s.getId()),
                        s.getTestDate().format(SHORT),
                        s.getPregnancyWeek() + "주차",
                        summaryOf(s, counts.getOrDefault(s.getId(), Map.of()))))
                .toList();
    }

    private String summaryOf(TestSheet sheet, Map<ResultStatus, Long> counts) {
        if (sheet.getSummaryForMom() != null && !sheet.getSummaryForMom().isBlank()) {
            return sheet.getSummaryForMom();
        }
        long danger = counts.getOrDefault(ResultStatus.DANGER, 0L);
        long caution = counts.getOrDefault(ResultStatus.CAUTION, 0L);

        if (danger > 0) return "확인이 필요한 항목이 %d개 있어요.\n선생님과 이야기해 보세요.".formatted(danger);
        if (caution > 0) return "주의해서 볼 항목이 %d개 있어요.".formatted(caution);
        return "이번 검사에서는 모든 항목이 정상 범위 안에 있어요.";
    }

    // ---------- 분석 탭 ----------

    @Transactional(readOnly = true)
    public List<AppDtos.TrendIndicator> getTrends(Long userId) {
        TestSheet latest = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusOrderByTestDateDesc(userId, AnalysisStatus.DONE)
                .orElse(null);
        if (latest == null) return List.of();

        List<TestResult> current = testResultRepository.findBySheetWithItem(latest.getId());

        List<Long> itemIds = current.stream()
                .map(TestResult::getTestItem).filter(Objects::nonNull)
                .map(TestItemCatalog::getId).distinct().toList();
        if (itemIds.isEmpty()) return List.of();

        Map<Long, List<TestResult>> histories = testResultRepository
                .findTrendsByItemIds(userId, itemIds).stream()
                .collect(Collectors.groupingBy(r -> r.getTestItem().getId()));

        return current.stream()
                .filter(r -> r.getTestItem() != null)
                .filter(r -> r.getResultStatus() != ResultStatus.UNKNOWN)
                // 꺾은선을 그릴 수 있는 항목만. 음성/양성에는 높낮이가 없고,
                // 면역검사의 cut-off 지수(0.07 등)는 추이로 볼 값이 아니다.
                .filter(r -> r.getTestItem().isTrendable()
                        && r.getTestItem().getResultType() == ResultType.NUMBER)
                .sorted(Comparator.comparingInt(r -> severity(r.getResultStatus())))
                .map(r -> toIndicator(r, histories.getOrDefault(r.getTestItem().getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppDtos.TrendIndicator getTrend(Long userId, String id) {
        return getTrends(userId).stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_TREND_DATA));
    }

    private AppDtos.TrendIndicator toIndicator(TestResult latest, List<TestResult> history) {
        TestItemCatalog item = latest.getTestItem();

        // 같은 날 여러 번 올렸으면 마지막 값만 쓴다. 그래프에 점이 겹치면 읽기 어렵다.
        LinkedHashMap<String, AppDtos.TrendPoint> byDate = new LinkedHashMap<>();
        history.stream()
                .filter(r -> r.getNumberValue() != null)
                .forEach(r -> byDate.put(
                        r.getTestDate().format(MONTH_DAY),
                        new AppDtos.TrendPoint(
                                r.getTestDate().format(MONTH_DAY), r.getNumberValue().doubleValue())));
        List<AppDtos.TrendPoint> points = List.copyOf(byDate.values());

        return new AppDtos.TrendIndicator(
                item.getCode().toLowerCase(Locale.ROOT),
                item.getNameKo(),
                statusLabel(latest.getResultStatus()),
                item.getUnit() == null ? "" : item.getUnit(),
                item.getBriefForMom() == null ? "" : item.getBriefForMom(),
                describeTrend(item, history),
                chartRange(item, points),
                ZONE_LABELS,
                points);
    }

    /** 차트 y축 범위. 정상 범위와 실제 값을 모두 담고 위아래로 여유를 둔다. */
    private List<Double> chartRange(TestItemCatalog item, List<AppDtos.TrendPoint> points) {
        List<Double> values = new ArrayList<>(points.stream().map(AppDtos.TrendPoint::value).toList());
        if (item.getNormalMin() != null) values.add(item.getNormalMin().doubleValue());
        if (item.getNormalMax() != null) values.add(item.getNormalMax().doubleValue());
        if (values.isEmpty()) return List.of(0.0, 100.0);

        double min = Collections.min(values);
        double max = Collections.max(values);
        double padding = Math.max((max - min) * 0.2, Math.abs(max) * 0.05 + 0.5);

        return List.of(round(Math.max(0, min - padding)), round(max + padding));
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /**
     * 추이 설명. 코드가 만든다.
     * 원인 단정이나 진단명은 넣지 않고 항상 의사 확인을 권하며 끝낸다.
     */
    private String describeTrend(TestItemCatalog item, List<TestResult> history) {
        String base = item.getBriefForMom() == null ? "" : item.getBriefForMom() + "\n";

        List<TestResult> numeric = history.stream()
                .filter(r -> r.getNumberValue() != null)
                .collect(java.util.stream.Collectors.toMap(
                        TestResult::getTestDate, r -> r, (a, b) -> b, LinkedHashMap::new))
                .values().stream().toList();

        if (numeric.size() < 2) {
            return base + "아직 비교할 이전 검사가 없어요.\n다음 검사 후에 변화를 보여드릴게요.";
        }

        BigDecimal first = numeric.get(0).getNumberValue();
        BigDecimal last = numeric.get(numeric.size() - 1).getNumberValue();

        String direction = switch (last.compareTo(first)) {
            case 1 -> "수치가 올라가는 모습이 보여요";
            case -1 -> "수치가 내려가는 모습이 보여요";
            default -> "수치가 비슷하게 유지되고 있어요";
        };

        return base
                + "최근 %d번의 검사에서 %s.\n".formatted(numeric.size(), direction)
                + "자연스러운 변화일 수도 있지만, 변화 폭이 괜찮은지\n의사 선생님과 함께 확인해보세요.";
    }

    // ---------- 공통 ----------

    private Map<Long, Map<ResultStatus, Long>> countByStatus(List<Long> sheetIds) {
        Map<Long, Map<ResultStatus, Long>> result = new HashMap<>();
        for (Object[] row : testResultRepository.countStatusBySheetIds(sheetIds)) {
            result.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((ResultStatus) row[1], (Long) row[2]);
        }
        return result;
    }

    private String statusLabel(ResultStatus status) {
        return switch (status) {
            case NORMAL -> "안심";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> "안심";
        };
    }

    private int severity(ResultStatus status) {
        return switch (status) {
            case DANGER -> 0;
            case CAUTION -> 1;
            case NORMAL -> 2;
            case UNKNOWN -> 3;
        };
    }
}

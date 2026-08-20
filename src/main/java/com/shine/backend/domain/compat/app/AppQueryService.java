package com.shine.backend.domain.compat.app;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import com.shine.backend.domain.user.dto.UserUpdateRequest;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.domain.user.service.UserService;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final UserService userService;
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

    /**
     * 개인정보 수정 — PATCH /api/v1/app/me
     *
     * PATCH /users/me 와 같은 일을 하되 응답을 GET /app/me 와 같은 모양으로 돌려준다.
     * 지금 프론트는 PATCH 응답 모양이 달라서 저장 후 /app/me 를 한 번 더 부르는데,
     * 이걸 쓰면 왕복이 한 번 줄어든다(전달사항 5번).
     */
    @Transactional
    public AppDtos.UserProfile updateProfile(Long userId, AppDtos.UserProfileUpdate request) {
        userService.update(userId, new UserUpdateRequest(
                request.name(),
                null,
                request.phone(),
                request.email(),
                request.guardianEmail(),
                request.extraEmail()));

        return getProfile(userId);
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
                // 지난 검사지를 나중에 올려도 타임라인 제자리에 꽂히도록 검사일로 정렬한다
                .sorted(Comparator.comparing(TestSheet::getTestDate)
                        .thenComparing(TestSheet::getId).reversed())
                .map(s -> new AppDtos.RecordEntry(
                        String.valueOf(s.getId()),
                        s.getTestDate().format(SHORT),
                        weekLabel(s.getPregnancyWeek()),
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
        // 최신 검사지에 있는 항목만 보면, 지난 검사에만 있던 항목의 추이를
        // 영영 볼 수 없게 된다. 지금까지 잰 모든 항목을 대상으로 한다.
        List<Long> itemIds = testResultRepository.findMeasuredItemIds(userId);
        if (itemIds.isEmpty()) return List.of();

        Map<Long, List<TestResult>> histories = testResultRepository
                .findTrendsByItemIds(userId, itemIds).stream()
                .collect(Collectors.groupingBy(r -> r.getTestItem().getId()));

        return histories.values().stream()
                // 항목마다 가장 최근에 제대로 판정된 측정을 대표값으로 쓴다.
                // 최신 값이 OCR 오독으로 미분류가 되어도 이전 값으로 그래프는 살린다.
                .map(h -> h.stream()
                        .filter(r -> r.getResultStatus() != ResultStatus.UNKNOWN)
                        .reduce((a, b) -> b).orElse(null))
                .filter(Objects::nonNull)
                // 꺾은선을 그릴 수 있는 항목만. 음성/양성에는 높낮이가 없고,
                // 면역검사의 cut-off 지수(0.07 등)는 추이로 볼 값이 아니다.
                .filter(r -> r.getTestItem().isTrendable()
                        && r.getTestItem().getResultType() == ResultType.NUMBER)
                .sorted(Comparator.comparingInt(r -> severity(r.getResultStatus())))
                .map(r -> toIndicator(r, histories.get(r.getTestItem().getId())))
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
        // 키를 연-월-일 전체로 둔다. 월·일만 쓰면 해가 바뀐 검사가 한 점으로 합쳐지고,
        // 12월 다음에 1월이 오는 순서도 지킬 수 없다.
        TreeMap<LocalDate, AppDtos.TrendPoint> byDate = new TreeMap<>();
        history.stream()
                .filter(r -> r.getNumberValue() != null)
                .filter(r -> r.getResultStatus() != ResultStatus.UNKNOWN)
                .forEach(r -> byDate.put(
                        r.getTestDate(),
                        new AppDtos.TrendPoint(
                                r.getTestDate().format(SHORT), r.getNumberValue().doubleValue())));
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
                .filter(r -> r.getResultStatus() != ResultStatus.UNKNOWN)
                .collect(java.util.stream.Collectors.toMap(
                        TestResult::getTestDate, r -> r, (a, b) -> b, TreeMap::new))
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

    /** 임신 시작 이전 검사지는 주차를 비워 보낸다. 음수를 화면에 띄울 수는 없다. */
    private String weekLabel(int week) {
        return week < 0 ? null : week + "주차";
    }

    /**
     * UNKNOWN을 "안심"으로 내려보내고 있었다.
     * 판정하지 못한 항목이 화면에서는 초록색 정상으로 보인다는 뜻이라,
     * 이 앱에서 가장 위험한 실패다. 이제 "확인 필요"로 나간다(전달사항 1번).
     */
    private String statusLabel(ResultStatus status) {
        return status.label();
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

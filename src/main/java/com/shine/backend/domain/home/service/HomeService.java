package com.shine.backend.domain.home.service;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.appointment.repository.AppointmentRepository;
import com.shine.backend.domain.home.dto.HomeResponse;
import com.shine.backend.domain.nutrition.NutritionRecommender;
import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.repository.QuestionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final String GREETING = "지금 내 몸은 어떻게\n변하고 있을까요?";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yy.MM.dd");
    private static final int WEEK_DAYS = 7;
    private static final int NUTRITION_SIZE = 8;
    private static final int QUESTION_SIZE = 3;

    private final UserRepository userRepository;
    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;
    private final QuestionRepository questionRepository;
    private final AppointmentRepository appointmentRepository;
    private final NutritionRecommender nutritionRecommender;

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();

        TestSheet sheet = testSheetRepository
                .findFirstByUserIdAndAnalysisStatusOrderByTestDateDesc(userId, AnalysisStatus.DONE)
                .orElse(null);

        List<TestResult> results = sheet == null ? List.of()
                : testResultRepository.findBySheetWithItem(sheet.getId());

        return new HomeResponse(
                new HomeResponse.UserSummary(
                        user.getName(),
                        user.getPregnancyWeek(today),
                        user.getPregnancyDay(today),
                        user.getDueDate()),
                GREETING,
                latestSheet(sheet, results),
                questions(userId, sheet),
                nutritions(results),
                weeklyCalendar(userId, today));
    }

    // ---------- 검사지 요약 ----------

    private HomeResponse.LatestSheet latestSheet(TestSheet sheet, List<TestResult> results) {
        if (sheet == null) return null;

        long danger = countOf(results, ResultStatus.DANGER);
        long caution = countOf(results, ResultStatus.CAUTION);

        return new HomeResponse.LatestSheet(
                sheet.getId(),
                sheet.getTestDate(),
                sheet.getTestDate().format(FMT),
                sheet.getPregnancyWeek(),
                summaryPreview(sheet, danger, caution),
                danger,
                caution,
                results.size());
    }

    /**
     * AI 요약이 없어도 화면이 비지 않도록 판정 개수로 문장을 만든다.
     * 고정 문장을 먼저 만들고 AI를 그 위에 얹는다.
     */
    private String summaryPreview(TestSheet sheet, long danger, long caution) {
        if (sheet.getSummaryForMom() != null && !sheet.getSummaryForMom().isBlank()) {
            return sheet.getSummaryForMom();
        }
        if (danger > 0) {
            return "확인이 필요한 항목이 %d개 있어요.\n선생님과 이야기해 보세요.".formatted(danger);
        }
        if (caution > 0) {
            return "주의해서 볼 항목이 %d개 있어요.\n자세한 내용을 확인해보세요.".formatted(caution);
        }
        return "이번 검사에서는 모든 항목이\n정상 범위 안에 있어요.";
    }

    // ---------- 질문 ----------

    private List<HomeResponse.QuestionSummary> questions(Long userId, TestSheet sheet) {
        if (sheet == null) return List.of();

        List<Question> found = questionRepository.findByTestSheetIdOrderByIdDesc(sheet.getId());

        return found.stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .limit(QUESTION_SIZE)
                .map(q -> new HomeResponse.QuestionSummary(
                        q.getId(), q.getContent(), q.getCreatedBy().name()))
                .toList();
    }

    // ---------- 추천 재료 ----------

    /** 정상 범위보다 낮게 나온 항목만 추천 근거로 쓴다. 다 정상이면 추천할 것이 없다. */
    private List<NutritionRecommender.Food> nutritions(List<TestResult> results) {
        Map<String, String> itemNames = new HashMap<>();
        Set<String> lowCodes = new LinkedHashSet<>();

        for (TestResult r : results) {
            TestItemCatalog item = r.getTestItem();
            if (item == null) continue;
            if (r.getResultStatus() != ResultStatus.DANGER
                    && r.getResultStatus() != ResultStatus.CAUTION) continue;

            // 낮아서 문제인 경우만. 높은 건 음식으로 해결할 일이 아니다.
            if (isBelowRange(r, item)) {
                lowCodes.add(item.getCode());
                itemNames.put(item.getCode(), item.getNameKo());
            }
        }

        return nutritionRecommender.recommend(lowCodes, itemNames, NUTRITION_SIZE);
    }

    private boolean isBelowRange(TestResult r, TestItemCatalog item) {
        if (r.getNumberValue() == null) return false;
        var min = r.getSheetNormalMin() != null && !item.isPregnancySpecific()
                ? r.getSheetNormalMin() : item.getNormalMin();
        return min != null && r.getNumberValue().compareTo(min) < 0;
    }

    // ---------- 주간 캘린더 ----------

    private List<HomeResponse.CalendarDay> weeklyCalendar(Long userId, LocalDate today) {
        var from = today.atStartOfDay();
        var to = today.plusDays(WEEK_DAYS - 1L).atTime(23, 59, 59);

        Map<LocalDate, List<Appointment>> byDate = appointmentRepository
                .findByUserIdAndVisitAtBetweenOrderByVisitAt(userId, from, to).stream()
                .collect(Collectors.groupingBy(a -> a.getVisitAt().toLocalDate()));

        List<HomeResponse.CalendarDay> days = new ArrayList<>();
        for (int i = 0; i < WEEK_DAYS; i++) {
            LocalDate date = today.plusDays(i);
            List<Appointment> appointments = byDate.getOrDefault(date, List.of());

            String label = appointments.stream()
                    .filter(a -> !a.isObgyn())
                    .map(Appointment::getTitle)
                    .findFirst()
                    .orElse(null);

            days.add(new HomeResponse.CalendarDay(
                    date,
                    date.getDayOfMonth(),
                    korean(date.getDayOfWeek()),
                    i == 0,
                    !appointments.isEmpty(),
                    label));
        }
        return days;
    }

    private String korean(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREAN);
    }

    private long countOf(List<TestResult> results, ResultStatus status) {
        return results.stream().filter(r -> r.getResultStatus() == status).count();
    }
}

package com.shine.backend.domain.home.dto;

import com.shine.backend.domain.nutrition.NutritionRecommender;

import java.time.LocalDate;
import java.util.List;

/**
 * 홈 화면 전체를 한 번에 준다.
 * 진입할 때마다 5번 호출하는 것보다 낫다.
 *
 * latestSheet가 null이면 검사지가 아직 없는 상태다.
 * 이때 questions와 nutritions도 비어 있으므로 화면은 빈 상태 문구를 띄우면 된다.
 */
public record HomeResponse(
        UserSummary user,
        String greeting,
        LatestSheet latestSheet,
        List<QuestionSummary> questions,
        List<NutritionRecommender.Food> nutritions,
        List<CalendarDay> weeklyCalendar
) {
    public record UserSummary(String name, int pregnancyWeek, int pregnancyDay, LocalDate dueDate) {}

    public record LatestSheet(
            Long testSheetId,
            LocalDate testDate,
            String displayDate,
            int pregnancyWeek,
            /** 홈 카드에 두 줄로 들어갈 요약. 서버가 잘라서 준다 */
            String summaryPreview,
            long danger,
            long caution,
            int total
    ) {}

    public record QuestionSummary(Long questionId, String content, String createdBy) {}

    public record CalendarDay(
            LocalDate date,
            int day,
            String dayOfWeek,
            boolean isToday,
            boolean hasAppointment,
            /** 산부인과가 아닌 일정만 제목을 짧게 표시한다 */
            String label
    ) {}
}

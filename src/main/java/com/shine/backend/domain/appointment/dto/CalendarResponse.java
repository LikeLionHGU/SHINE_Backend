package com.shine.backend.domain.appointment.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 월 캘린더.
 * 날짜를 주 단위로 묶는 건 프론트가 한다. 서버는 각 날짜의 임신 주차와 마커만 준다.
 * (화면 좌측의 "8주차, 9주차" 라벨은 그 주 첫 날짜의 pregnancyWeek를 쓰면 된다)
 */
public record CalendarResponse(
        String yearMonth,
        List<Day> days,
        List<AppointmentResponse> appointments
) {
    public record Day(
            LocalDate date,
            int pregnancyWeek,
            boolean isToday,
            List<Marker> markers
    ) {}

    public record Marker(
            Long appointmentId,
            boolean isObgyn,
            /** 지난 일정이면 채운 점(●), 아니면 빈 점(○) */
            boolean visited,
            /** 산부인과가 아닌 일정만 라벨을 표시한다 */
            String label
    ) {}
}

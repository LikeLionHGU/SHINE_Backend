package com.shine.backend.domain.appointment.dto;

import com.shine.backend.domain.appointment.entity.Appointment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record AppointmentResponse(
        Long appointmentId,
        String title,
        String location,
        LocalDateTime visitAt,
        String displayDate,
        String displayTime,
        boolean isObgyn,
        int pregnancyWeek,
        /** 지난 일정이면 true. 캘린더에서 채운 점(●) / 빈 점(○)을 가른다. */
        boolean visited,
        long questionCount
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yy.MM.dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    public static AppointmentResponse from(Appointment a, LocalDateTime now, long questionCount) {
        return new AppointmentResponse(
                a.getId(),
                a.getTitle(),
                a.getLocation(),
                a.getVisitAt(),
                a.getVisitAt().format(DATE_FMT),
                a.getVisitAt().format(TIME_FMT),
                a.isObgyn(),
                a.getPregnancyWeek(),
                a.getVisitAt().isBefore(now),
                questionCount);
    }
}

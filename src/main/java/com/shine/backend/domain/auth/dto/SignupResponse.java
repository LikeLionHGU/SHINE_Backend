package com.shine.backend.domain.auth.dto;

import com.shine.backend.domain.user.entity.User;

import java.time.LocalDate;

public record SignupResponse(
        Long userId,
        String name,
        int pregnancyWeek,
        LocalDate lastPeriodDate
) {
    public static SignupResponse from(User user, LocalDate today) {
        return new SignupResponse(
                user.getId(), user.getName(),
                user.getPregnancyWeek(today), user.getLastPeriodDate());
    }
}

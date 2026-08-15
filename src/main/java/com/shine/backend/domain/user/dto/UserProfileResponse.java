package com.shine.backend.domain.user.dto;

import com.shine.backend.domain.user.entity.User;

import java.time.LocalDate;

public record UserProfileResponse(
        Long userId,
        String name,
        String nickname,
        String profileImageUrl,
        String loginId,
        String phoneNumber,
        String email,
        String guardianEmail,
        String additionalEmail,
        LocalDate lastPeriodDate,
        int pregnancyWeek,
        int pregnancyDay,
        LocalDate dueDate,
        Settings settings
) {
    public record Settings(boolean cameraAgreed, boolean notificationEnabled) {}

    public static UserProfileResponse from(User user, LocalDate today) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getNickname(),
                null,
                user.getLoginId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getGuardianEmail(),
                user.getAdditionalEmail(),
                user.getLastPeriodDate(),
                user.getPregnancyWeek(today),
                user.getPregnancyDay(today),
                user.getDueDate(),
                new Settings(user.isCameraAgreed(), user.isNotificationEnabled()));
    }
}

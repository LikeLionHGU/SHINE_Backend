package com.shine.backend.domain.auth.dto;

import com.shine.backend.domain.user.entity.User;

import java.time.LocalDate;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(
            Long userId,
            String name,
            int pregnancyWeek,
            int pregnancyDay,
            boolean hasGuardianEmail
    ) {
        public static UserSummary from(User user, LocalDate today) {
            return new UserSummary(
                    user.getId(),
                    user.getName(),
                    user.getPregnancyWeek(today),
                    user.getPregnancyDay(today),
                    user.getGuardianEmail() != null);
        }
    }

    public static LoginResponse of(TokenResponse token, User user, LocalDate today) {
        return new LoginResponse(
                token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresIn(),
                UserSummary.from(user, today));
    }
}

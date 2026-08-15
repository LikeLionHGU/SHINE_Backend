package com.shine.backend.domain.user.service;

import com.shine.backend.domain.user.dto.*;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(Long userId) {
        return UserProfileResponse.from(find(userId), LocalDate.now());
    }

    @Transactional
    public UserProfileResponse update(Long userId, UserUpdateRequest request) {
        User user = find(userId);

        if (request.email() != null && !request.email().equals(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        user.updateProfile(request.name(), request.nickname(), request.phoneNumber(), request.email());
        user.updateEmails(request.guardianEmail(), request.additionalEmail());

        return UserProfileResponse.from(user, LocalDate.now());
    }

    @Transactional
    public UserProfileResponse updatePregnancy(Long userId, PregnancyUpdateRequest request) {
        User user = find(userId);
        LocalDate today = LocalDate.now();

        if (request.lastPeriodDate() != null) {
            user.updateLastPeriodDate(request.lastPeriodDate());
        } else if (request.pregnancyWeek() != null) {
            user.updateLastPeriodDate(User.toLastPeriodDate(request.pregnancyWeek(), today));
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "임신 주차 또는 최종 월경일이 필요합니다.");
        }

        return UserProfileResponse.from(user, today);
    }

    @Transactional
    public UserProfileResponse.Settings updateSettings(Long userId, SettingsUpdateRequest request) {
        User user = find(userId);
        user.updateSettings(request.cameraAgreed(), request.notificationEnabled());
        return new UserProfileResponse.Settings(user.isCameraAgreed(), user.isNotificationEnabled());
    }

    /** 의료 데이터라 즉시 삭제한다. 자식 레코드는 FK CASCADE로 함께 지워진다. */
    @Transactional
    public void withdraw(Long userId, String password) {
        User user = find(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        userRepository.delete(user);
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}

package com.shine.backend.domain.auth.service;

import com.shine.backend.domain.auth.dto.*;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.domain.user.repository.UserRepository;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import com.shine.backend.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 중복 확인 API와 별개로 여기서 한 번 더 본다 (동시 가입 경합 방지)
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .guardianEmail(request.guardianEmail())
                // 주차를 저장하지 않고 최종월경일로 역산해 보관한다(설계결정①)
                .lastPeriodDate(User.toLastPeriodDate(request.pregnancyWeek(), today))
                .cameraAgreed(false)
                .notificationEnabled(true)
                .termsAgreedAt(now)
                .privacyAgreedAt(now)
                .sensitiveAgreedAt(now)
                .loginFailCount(0)
                .build();

        userRepository.save(user);

        // TODO STEP 6: prenatal_schedule.json 기반으로 검진 일정 자동 생성
        return SignupResponse.from(user, today);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByLoginId(request.loginId())
                // 아이디가 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다.
                // 구분하면 어떤 아이디가 존재하는지 알려주는 셈이 된다.
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (user.isLocked(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.loginFailed(now);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        user.loginSucceeded();

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = null;

        // '자동 로그인'을 체크했을 때만 리프레시를 발급한다
        if (request.isAutoLogin()) {
            refreshToken = jwtProvider.createRefreshToken(user.getId());
            refreshTokenService.save(user.getId(), refreshToken, jwtProvider.getRefreshValidityMs());
        }

        TokenResponse token = TokenResponse.of(accessToken, refreshToken, jwtProvider.getAccessValidityMs());
        return LoginResponse.of(token, user, LocalDate.now());
    }

    /** Rotation — 새 리프레시를 주고 기존 것은 폐기한다. 재사용이 감지되면 전부 무효화된다. */
    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {
        Long userId = jwtProvider.parseRefreshToken(refreshToken);
        refreshTokenService.validateOrThrow(userId, refreshToken);

        String newAccess = jwtProvider.createAccessToken(userId);
        String newRefresh = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, newRefresh, jwtProvider.getRefreshValidityMs());

        return TokenResponse.of(newAccess, newRefresh, jwtProvider.getAccessValidityMs());
    }

    public void logout(Long userId, String accessToken) {
        refreshTokenService.delete(userId);
        refreshTokenService.blacklistAccessToken(accessToken, jwtProvider.getRemainingMs(accessToken));
    }
}

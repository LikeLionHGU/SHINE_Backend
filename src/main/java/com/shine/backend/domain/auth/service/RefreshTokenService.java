package com.shine.backend.domain.auth.service;

import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 리프레시 토큰을 Redis에 보관한다.
 *
 * Rotation을 적용한다 — 재발급할 때마다 새 토큰을 주고 기존 것은 폐기한다.
 * 이미 폐기된 토큰이 다시 들어오면 탈취를 의심하고 해당 사용자의 토큰을 전부 무효화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    public void save(Long userId, String refreshToken, long validityMs) {
        redis.opsForValue().set(
                REFRESH_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(validityMs));
    }

    /**
     * 저장된 토큰과 일치하는지 확인한다.
     * 불일치면 이미 rotation된 옛 토큰이 재사용된 것 → 전부 폐기하고 재로그인을 요구한다.
     */
    public void validateOrThrow(Long userId, String refreshToken) {
        String stored = redis.opsForValue().get(REFRESH_PREFIX + userId);

        if (stored == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!stored.equals(refreshToken)) {
            log.warn("리프레시 토큰 재사용 감지 userId={}", userId);
            delete(userId);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }
    }

    public void delete(Long userId) {
        redis.delete(REFRESH_PREFIX + userId);
    }

    /** 로그아웃한 액세스 토큰은 남은 만료 시간만큼 블랙리스트에 둔다. */
    public void blacklistAccessToken(String accessToken, long remainingMs) {
        if (remainingMs <= 0) return;
        redis.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(remainingMs));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + accessToken));
    }
}

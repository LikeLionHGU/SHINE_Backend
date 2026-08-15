package com.shine.backend.global.jwt;

import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** 토큰 발급과 검증만 담당한다. 저장·폐기는 RefreshTokenService가 맡는다. */
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;

    @Getter
    private final long accessValidityMs;

    @Getter
    private final long refreshValidityMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshValidityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValidityMs = accessValidityMs;
        this.refreshValidityMs = refreshValidityMs;
    }

    public String createAccessToken(Long userId) {
        return create(userId, TYPE_ACCESS, accessValidityMs);
    }

    public String createRefreshToken(Long userId) {
        return create(userId, TYPE_REFRESH, refreshValidityMs);
    }

    private String create(Long userId, String type, long validityMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMs))
                .signWith(key)
                .compact();
    }

    /** 액세스 토큰에서 userId를 꺼낸다. 만료·위조면 예외를 던진다. */
    public Long parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(claims.getSubject());
    }

    public Long parseRefreshToken(String token) {
        Claims claims;
        try {
            claims = parse(token);
        } catch (BusinessException e) {
            // 리프레시가 만료되면 재로그인이 필요하다는 뜻이라 메시지를 바꾼다
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return Long.valueOf(claims.getSubject());
    }

    /** 로그아웃 시 블랙리스트 TTL을 계산하려면 남은 만료 시간이 필요하다. */
    public long getRemainingMs(String token) {
        try {
            Date exp = parse(token).getExpiration();
            return Math.max(0, exp.getTime() - System.currentTimeMillis());
        } catch (BusinessException e) {
            return 0;
        }
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}

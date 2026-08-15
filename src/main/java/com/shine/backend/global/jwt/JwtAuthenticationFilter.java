package com.shine.backend.global.jwt;

import com.shine.backend.domain.auth.service.RefreshTokenService;
import com.shine.backend.global.exception.BusinessException;
import com.shine.backend.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Authorization 헤더의 토큰을 검증하고 SecurityContext에 userId를 넣는다.
 * 컨트롤러에서는 @AuthenticationPrincipal Long userId 로 꺼내 쓴다.
 *
 * 필터는 DispatcherServlet보다 먼저 돌기 때문에 GlobalExceptionHandler가 잡지 못한다.
 * 그래서 여기서 직접 응답을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰이 없으면 그냥 통과시킨다. 인증이 필요한 경로면 뒤에서 401이 난다.
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (refreshTokenService.isBlacklisted(token)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }

            Long userId = jwtProvider.parseAccessToken(token);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId, null, List.of()));

        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            ErrorResponseWriter.write(response, e.getErrorCode());
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) return null;
        String token = header.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}

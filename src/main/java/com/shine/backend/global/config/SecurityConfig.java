package com.shine.backend.global.config;

import com.shine.backend.global.exception.ErrorCode;
import com.shine.backend.global.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /** 인증 없이 열어두는 경로 */
    private static final String[] PUBLIC = {
            // auth 전체를 열면 logout까지 열린다. 필요한 것만 명시한다.
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/auth/check-login-id",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 이 줄이 없으면 아래 CorsConfigurationSource 빈이 무시된다
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // JWT를 쓰므로 서버가 세션을 만들지 않는다
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Authorization 헤더를 쓰면 모든 요청 앞에 preflight OPTIONS가 붙는다.
                        // 인증 대상으로 두면 401로 튕겨서 본 요청이 아예 안 나간다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 인증 실패도 다른 API와 같은 형식으로 응답한다
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> write(res, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> write(res, ErrorCode.FORBIDDEN)));

        return http.build();
    }

    /**
     * Expo Web(localhost:8081)에서 붙기 때문에 CORS가 필요하다.
     * 네이티브 앱에는 CORS가 없지만 웹으로 띄우면 브라우저가 막는다.
     *
     * allowCredentials(true)와 allowedOrigins("*")는 함께 쓸 수 없어
     * allowedOriginPatterns를 쓴다.
     *
     * 데모 기간이라 넓게 열어뒀다. 실서비스에서는 배포 도메인만 남겨야 한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void write(HttpServletResponse res, ErrorCode code) throws IOException {
        res.setStatus(code.getStatus().value());
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("""
                {"success":false,"code":"%s","message":"%s","data":null}"""
                .formatted(code.name(), code.getMessage()));
    }

    /** BCrypt strength 10. 해시 결과가 항상 60자라 컬럼도 VARCHAR(60)으로 잡았다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

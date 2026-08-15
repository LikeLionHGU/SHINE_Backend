package com.shine.backend.global.jwt;

import com.shine.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 필터 단계에서 쓰는 에러 응답.
 * ObjectMapper를 주입받지 않는다 — 형태가 고정이라 직접 쓰는 편이 단순하고,
 * Jackson 버전 변경(Boot 4의 Jackson 3 전환)에도 영향받지 않는다.
 */
final class ErrorResponseWriter {

    private ErrorResponseWriter() {}

    static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"success":false,"code":"%s","message":"%s","data":null}"""
                .formatted(code.name(), code.getMessage()));
    }

    static void writePublic(HttpServletResponse response, ErrorCode code) throws IOException {
        write(response, code);
    }
}

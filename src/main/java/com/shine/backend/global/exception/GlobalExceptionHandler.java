package com.shine.backend.global.exception;

import com.shine.backend.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** 모든 예외를 ApiResponse 형태로 변환한다. 컨트롤러에서 try-catch를 쓰지 않게 하는 것이 목적. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[{}] {}", code.name(), e.getMessage());
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code));
    }

    /** @Valid 검증 실패 — 어느 필드가 왜 틀렸는지 data에 담아 프론트가 인라인 표시할 수 있게 한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "reason", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();

        ErrorCode code = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code, Map.of("errors", errors)));
    }

    /** 예상 못 한 예외. 내부 메시지를 사용자에게 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code));
    }
}

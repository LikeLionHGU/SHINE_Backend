package com.shine.backend.global.exception;

import com.shine.backend.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

        // 상황을 설명하는 메시지가 붙어 있으면 그걸 보여준다.
        // 기본 문구("입력값이 올바르지 않습니다")만으로는 무엇을 고쳐야 할지 알 수 없다.
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? code.getMessage() : e.getMessage();

        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(false, code.name(), message, null));
    }

    /**
     * @Valid 검증 실패 — 어느 필드가 왜 틀렸는지 data에 담는다.
     * MethodArgumentNotValidException 은 BindException 의 하위라 함께 받는다.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(BindException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField() == null ? "" : fe.getField(),
                        "reason", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
                .toList();

        log.warn("검증 실패 {}", errors);
        return invalid(errors);
    }

    /** 필수 쿼리 파라미터 누락. 핸들러가 없으면 500으로 샌다. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(
            MissingServletRequestParameterException e) {
        return invalid(List.of(Map.of(
                "field", e.getParameterName(),
                "reason", "필수 항목입니다.")));
    }

    /** 숫자 자리에 글자가 오는 등 타입이 맞지 않는 경우. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        return invalid(List.of(Map.of(
                "field", e.getName(),
                "reason", "형식이 올바르지 않습니다.")));
    }

    private ResponseEntity<ApiResponse<Object>> invalid(List<Map<String, String>> errors) {
        ErrorCode code = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code, Map.of("errors", errors)));
    }

    /**
     * 요청 본문이 JSON으로 해석되지 않는 경우.
     * 핸들러가 없으면 아래 fallback으로 내려가 500이 되고,
     * 사용자에게 "서버 오류"가 노출된다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 해석 실패: {}", e.getMessage());
        return invalid(List.of(Map.of(
                "field", "body",
                "reason", "요청 형식을 확인해주세요.")));
    }

    /** 예상 못 한 예외. 내부 메시지를 사용자에게 노출하지 않는다. */
    /**
     * 파일 파트를 못 찾았을 때.
     *
     * 여태 이게 아래 Exception 핸들러에 걸려 500으로 나갔다. 클라이언트 요청이
     * 잘못된 것인데 서버 장애처럼 보여서, 앱에서 사진이 안 올라가는 원인을
     * 찾는 데 한참 걸렸다. 무엇이 빠졌는지 이름을 그대로 알려준다.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingPart(MissingServletRequestPartException e) {
        ErrorCode code = ErrorCode.MISSING_FILE_PART;
        log.warn("파일 파트 누락 [{}]", e.getRequestPartName());

        return ResponseEntity.status(code.getStatus()).body(new ApiResponse<>(
                false, code.name(),
                "파일을 찾을 수 없습니다. '" + e.getRequestPartName() + "' 이름으로 보내주세요.",
                null));
    }

    /** 업로드 용량 초과. 이것도 500으로 나가면 사용자가 이유를 알 수 없다. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleTooLarge(MaxUploadSizeExceededException e) {
        ErrorCode code = ErrorCode.FILE_TOO_LARGE;
        log.warn("업로드 용량 초과", e);

        return ResponseEntity.status(code.getStatus())
                .body(new ApiResponse<>(false, code.name(), code.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        // 어떤 예외가 새는지 클래스 이름까지 남긴다. 이게 없으면 원인을 못 찾는다.
        log.error("처리되지 않은 예외 [{}]", e.getClass().getName(), e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code));
    }
}

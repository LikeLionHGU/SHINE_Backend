package com.shine.backend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shine.backend.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API가 이 형태로 응답한다.
 * 성공/실패가 같은 껍데기라 프론트는 success 하나만 보고 분기하면 된다.
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "요청에 성공했습니다.", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "SUCCESS", "요청에 성공했습니다.", null);
    }

    public static ApiResponse<Object> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.name(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Object> error(ErrorCode errorCode, Object data) {
        return new ApiResponse<>(false, errorCode.name(), errorCode.getMessage(), data);
    }
}

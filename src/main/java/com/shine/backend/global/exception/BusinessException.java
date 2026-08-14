package com.shine.backend.global.exception;

import lombok.Getter;

/** 의도적으로 발생시키는 예외. GlobalExceptionHandler가 ErrorCode를 보고 응답을 만든다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}

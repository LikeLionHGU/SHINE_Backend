package com.shine.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 목록.
 * enum 이름이 그대로 응답의 code 값이 되므로, 프론트는 이 이름으로 분기한다.
 * 메시지는 사용자에게 그대로 보여줄 수 있는 문장으로 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ---------- 공통 ----------
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // ---------- 인증 ----------
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    // 아이디가 없는 경우와 비밀번호가 틀린 경우를 구분하지 않는다. 계정 존재 여부가 유출되면 안 된다.
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 많아 잠시 잠겼습니다. 10분 후 다시 시도해주세요."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "보안상의 이유로 로그아웃되었습니다."),

    // ---------- 사용자 ----------
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PREGNANCY_WEEK(HttpStatus.BAD_REQUEST, "임신 주차는 1~42 사이여야 합니다."),

    // ---------- 검사지 ----------
    SHEET_NOT_FOUND(HttpStatus.NOT_FOUND, "검사지를 찾을 수 없습니다."),
    SHEET_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 검사지가 아닙니다."),
    ANALYSIS_NOT_DONE(HttpStatus.CONFLICT, "아직 분석이 끝나지 않았습니다."),
    ANALYSIS_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 분석이 진행 중입니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "검사지는 한 번에 최대 5장까지 올릴 수 있어요."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "검사 항목을 찾을 수 없습니다."),
    RESULT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "결과 형식이 맞지 않습니다."),
    VALUE_OUT_OF_HARD_LIMIT(HttpStatus.BAD_REQUEST, "입력한 수치가 정상적인 범위를 크게 벗어났어요. 다시 확인해주세요."),

    // ---------- 분석 ----------
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "검사 항목을 찾을 수 없습니다."),
    NO_TREND_DATA(HttpStatus.NOT_FOUND, "아직 비교할 검사 기록이 없어요."),

    // ---------- 일정 · 공유 ----------
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    VISIT_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 일정이 아닙니다."),
    TEST_DATE_IN_FUTURE(HttpStatus.BAD_REQUEST, "검사일이 오늘 이후입니다. 날짜를 다시 확인해주세요."),
    INVALID_VISIT_DATE(HttpStatus.BAD_REQUEST, "방문 일시가 올바르지 않습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    GUARDIAN_EMAIL_NOT_SET(HttpStatus.BAD_REQUEST, "보호자 이메일을 먼저 등록해주세요."),
    SHARE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘은 더 이상 공유할 수 없어요. 내일 다시 시도해주세요."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "메일 전송에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}

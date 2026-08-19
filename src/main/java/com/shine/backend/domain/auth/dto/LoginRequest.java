package com.shine.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "아이디를 입력해주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        /**
         * 로그인 화면의 '자동 로그인' 체크박스. true일 때만 리프레시 토큰을 발급한다.
         * 필드를 생략해도 요청이 실패하지 않도록 래퍼 타입으로 둔다.
         */
        Boolean autoLogin
) {
    public boolean isAutoLogin() {
        return Boolean.TRUE.equals(autoLogin);
    }
}

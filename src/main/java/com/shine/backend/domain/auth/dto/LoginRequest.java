package com.shine.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "아이디를 입력해주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        /** 로그인 화면의 '자동 로그인' 체크박스. true일 때만 리프레시 토큰을 발급한다. */
        boolean autoLogin
) {}

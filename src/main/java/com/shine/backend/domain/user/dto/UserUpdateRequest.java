package com.shine.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 부분 수정. 보낸 필드만 반영된다. */
public record UserUpdateRequest(

        @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
        String name,

        @Size(max = 30)
        String nickname,

        @Pattern(regexp = "^[+0-9][0-9 -]{7,19}$", message = "휴대폰 번호 형식이 올바르지 않아요.")
        String phoneNumber,

        @Email(message = "이메일 형식이 올바르지 않아요.")
        String email,

        @Email(message = "보호자 이메일 형식이 올바르지 않아요.")
        String guardianEmail,

        @Email(message = "이메일 형식이 올바르지 않아요.")
        String additionalEmail
) {}

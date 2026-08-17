package com.shine.backend.domain.auth.dto;

import jakarta.validation.constraints.*;

/**
 * pregnancyWeek는 저장하지 않는다. 서버가 lastPeriodDate로 역산한다.
 *
 * 동의 3종은 모두 필수다. 건강정보는 개인정보보호법상 민감정보라
 * 일반 개인정보 동의와 별도로 받아야 하며, "전체 동의" 하나로 묶으면 안 된다.
 */
public record SignupRequest(

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
        String name,

        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 4~20자의 영문 소문자와 숫자만 사용할 수 있어요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$",
                message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 해요.")
        String password,

        @NotNull(message = "임신 주차를 선택해주세요.")
        @Min(value = 1, message = "임신 주차는 1~42 사이여야 해요.")
        @Max(value = 42, message = "임신 주차는 1~42 사이여야 해요.")
        Integer pregnancyWeek,

        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        @Pattern(regexp = "^[+0-9][0-9 -]{7,19}$", message = "휴대폰 번호 형식이 올바르지 않아요.")
        String phoneNumber,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        String email,

        @Email(message = "보호자 이메일 형식이 올바르지 않아요.")
        String guardianEmail
) {}

// 약관 동의 필드는 화면이 없어 일단 제외한다.
// 서버가 가입 시각으로 채우며, 실제 서비스 전에는 termsAgreed / privacyAgreed /
// sensitiveAgreed 를 되살려야 한다. 건강정보는 민감정보라 별도 동의가 필요하다.

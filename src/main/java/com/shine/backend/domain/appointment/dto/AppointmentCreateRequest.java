package com.shine.backend.domain.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 50, message = "제목은 50자 이내로 입력해주세요.")
        String title,

        @Size(max = 100)
        String location,

        @NotNull(message = "날짜와 시간을 선택해주세요.")
        LocalDateTime visitAt,

        /** 화면의 '산부인과' 토글. 이 값이 true인 일정에만 질문이 붙는다. */
        boolean isObgyn
) {}

package com.shine.backend.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 진료 때 물어볼 질문을 적어두는 메모다.
 * AI에게 답변을 받는 챗봇이 아니라서 서버는 답을 생성하지 않는다.
 */
public record QuestionCreateRequest(

        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = 500, message = "질문은 500자 이내로 입력해주세요.")
        String content,

        /** 어떤 검사지를 보고 생긴 질문인지 */
        Long testSheetId,

        /** 어느 진료에서 물어볼지 */
        Long appointmentId
) {}

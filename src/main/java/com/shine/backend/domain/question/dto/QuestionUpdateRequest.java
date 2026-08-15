package com.shine.backend.domain.question.dto;

import com.shine.backend.domain.question.entity.QuestionStatus;
import jakarta.validation.constraints.Size;

public record QuestionUpdateRequest(

        @Size(max = 500) String content,

        /** 진료 후 사용자가 기록하는 의사 답변 */
        @Size(max = 1000) String doctorAnswer,

        QuestionStatus questionStatus
) {}

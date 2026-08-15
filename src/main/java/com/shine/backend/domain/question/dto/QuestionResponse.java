package com.shine.backend.domain.question.dto;

import com.shine.backend.domain.question.entity.Question;
import com.shine.backend.domain.question.entity.QuestionSource;
import com.shine.backend.domain.question.entity.QuestionStatus;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long questionId,
        String content,
        /** AI = 검사 결과를 보고 추천한 질문 문구. USER = 직접 입력 */
        QuestionSource createdBy,
        QuestionStatus questionStatus,
        String doctorAnswer,
        Long testSheetId,
        Long appointmentId,
        LocalDateTime createdAt
) {
    public static QuestionResponse from(Question q) {
        return new QuestionResponse(
                q.getId(),
                q.getContent(),
                q.getCreatedBy(),
                q.getQuestionStatus(),
                q.getDoctorAnswer(),
                q.getTestSheet() == null ? null : q.getTestSheet().getId(),
                q.getAppointment() == null ? null : q.getAppointment().getId(),
                q.getCreatedAt());
    }
}

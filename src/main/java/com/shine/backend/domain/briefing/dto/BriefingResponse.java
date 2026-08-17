package com.shine.backend.domain.briefing.dto;

import com.shine.backend.domain.question.dto.QuestionResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 진료 상세 화면 — 기능 2(3초 닥터 브리핑).
 *
 * 진료일을 기준으로 검사지를 둘로 나눈다.
 *   todaySheet    그 날짜에 받은 검사지. 진료 전이면 없다
 *   previousSheet 그보다 앞선 가장 최근 검사지. 수치 비교의 기준이 된다
 */
public record BriefingResponse(
        Long appointmentId,
        String title,
        String location,
        LocalDateTime visitAt,
        String displayDate,
        String displayTime,
        boolean isObgyn,
        boolean visited,
        int pregnancyWeek,
        SheetSummary todaySheet,
        SheetSummary previousSheet,
        List<QuestionResponse> questions,
        /** 검사지도 질문도 없을 때 화면에 띄울 문구 */
        String emptyMessage
) {
    public record SheetSummary(
            Long testSheetId,
            LocalDate testDate,
            String displayDate,
            int pregnancyWeek,
            String summaryPreview,
            long danger,
            long caution,
            int total
    ) {}
}

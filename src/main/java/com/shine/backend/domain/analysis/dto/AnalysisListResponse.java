package com.shine.backend.domain.analysis.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 분석 탭.
 * prevDate/nextDate는 화면 상단의 `< 8월 1일 >` 좌우 이동에 쓴다. 없으면 null.
 */
public record AnalysisListResponse(
        Long testSheetId,
        LocalDate testDate,
        String displayDate,
        int pregnancyWeek,
        LocalDate prevDate,
        LocalDate nextDate,
        List<AnalysisItemResponse> items
) {
    public static AnalysisListResponse empty() {
        return new AnalysisListResponse(null, null, null, 0, null, null, List.of());
    }
}

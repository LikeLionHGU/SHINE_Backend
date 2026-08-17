package com.shine.backend.domain.compat.dto;

import java.util.List;

/**
 * 프론트의 LastReport 와 맞춘 응답.
 * AsyncStorage에 저장하던 것을 그대로 대체할 수 있다.
 *
 * items의 status는 서버가 임신 기준으로 다시 계산한 값이다.
 * 검사지에 인쇄된 참고치는 비임신 성인 기준인 경우가 많아,
 * 그대로 쓰면 정상인 산모가 "주의"로 나온다.
 */
public record ReportResponse(
        Long testSheetId,
        /** "YY.MM.DD" */
        String testDate,
        boolean testDateConfirmed,
        String week,
        List<ParsedTestItemDto> items,
        String summary,
        List<String> questions,
        List<ReportUploadRequest.FoodDto> foods
) {}

package com.shine.backend.domain.compat.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 프론트가 OCR을 끝낸 뒤 서버로 보내는 검사지.
 *
 * 서버는 이걸 받아서
 *   ① 항목을 카탈로그와 매칭하고
 *   ② 임신 기준으로 판정을 다시 계산하고
 *   ③ 항목별로 저장해 추이 그래프와 기록 탭을 채운다
 */
public record ReportUploadRequest(
        /** "YY.MM.DD" 또는 "yyyy-MM-dd". 없으면 오늘로 두고 확인 필요 표시 */
        String testDate,

        @NotEmpty(message = "검사 항목이 비어 있습니다.")
        List<ParsedTestItemDto> items,

        /** 프론트가 만든 종합 소견 */
        String summary,

        /** 프론트가 만든 추천 질문 */
        List<String> questions,

        /** 프론트가 만든 추천 음식 */
        List<FoodDto> foods
) {
    public record FoodDto(String name, String reason) {}
}

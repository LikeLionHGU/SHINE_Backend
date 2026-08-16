package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;

import java.time.LocalDate;

/**
 * 폴링 응답.
 *
 * testDateConfirmRequired가 true면 분석은 끝났지만 검사일을 모르는 상태다.
 * 검사일이 틀리면 추이 그래프의 X축이 통째로 어긋나므로 오늘 날짜로 대충 넣으면 안 된다.
 */
public record AnalysisStatusResponse(
        Long testSheetId,
        AnalysisStatus analysisStatus,
        String message,
        Integer pollIntervalMs,
        String failureReason,
        Boolean retryable,
        boolean testDateConfirmRequired,
        LocalDate testDate
) {
    private static final String ANALYZING_MESSAGE =
            "업로드한 사진을 스캔 후\n검사 결과를 보기 쉽게 바꾸는 중이에요";

    public static AnalysisStatusResponse from(TestSheet sheet) {
        return switch (sheet.getAnalysisStatus()) {
            case WAITING, ANALYZING -> new AnalysisStatusResponse(
                    sheet.getId(), sheet.getAnalysisStatus(), ANALYZING_MESSAGE,
                    2000, null, null, false, null);

            case DONE -> new AnalysisStatusResponse(
                    sheet.getId(), sheet.getAnalysisStatus(), null,
                    null, null, null,
                    !sheet.isTestDateConfirmed(), sheet.getTestDate());

            case FAILED -> new AnalysisStatusResponse(
                    sheet.getId(), sheet.getAnalysisStatus(),
                    failureMessage(sheet.getFailureReason()),
                    null, sheet.getFailureReason(), true, false, null);
        };
    }

    /** 실패 사유별 사용자 안내 문구. 그대로 화면에 띄우면 된다. */
    private static String failureMessage(String reason) {
        if (reason == null) return "분석에 실패했어요. 다시 시도해주세요.";
        return switch (reason) {
            case "IMAGE_UNREADABLE" -> "글씨를 알아보기 어려워요. 밝은 곳에서 다시 찍어주세요.";
            case "NOT_A_TEST_SHEET" -> "검사 결과지가 아닌 것 같아요. 다시 확인해주세요.";
            case "NO_ITEM_MATCHED" -> "검사 항목을 찾지 못했어요. 원본은 저장해두었어요.";
            case "OCR_ENGINE_ERROR" -> "일시적인 오류예요. 잠시 후 다시 시도해주세요.";
            case "LLM_ERROR" -> "설명을 만들지 못했어요. 검사 결과는 확인할 수 있어요.";
            default -> "분석에 실패했어요. 다시 시도해주세요.";
        };
    }
}

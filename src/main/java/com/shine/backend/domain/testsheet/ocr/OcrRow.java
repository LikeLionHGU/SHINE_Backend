package com.shine.backend.domain.testsheet.ocr;

import java.math.BigDecimal;

/**
 * 검사지 표의 한 줄.
 *
 * 실제 검사지 컬럼: 검사분야 | 검사명 | 결과 | 단위 | 참고치 | 판정
 * bbox는 원본 이미지 위 하이라이트용이며 0~1 정규화 비율이다.
 */
public record OcrRow(
        String category,
        String label,
        String value,
        String unit,
        String referenceRange,
        String verdict,
        Integer bboxPage,
        BigDecimal bboxX,
        BigDecimal bboxY,
        BigDecimal bboxWidth,
        BigDecimal bboxHeight
) {
    public static OcrRow of(String category, String label, String value,
                            String unit, String referenceRange, String verdict) {
        return new OcrRow(category, label, value, unit, referenceRange, verdict,
                null, null, null, null, null);
    }
}

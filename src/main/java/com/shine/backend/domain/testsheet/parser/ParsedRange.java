package com.shine.backend.domain.testsheet.parser;

import java.math.BigDecimal;

/**
 * 검사지에 인쇄된 참고치를 해석한 결과.
 *
 * @param min      수치 범위의 하한. 열거형이면 null
 * @param max      수치 범위의 상한
 * @param rawText  원문. 열거형("양성,음성,약양성")은 판정에 쓰지 않고 보존만 한다
 */
public record ParsedRange(BigDecimal min, BigDecimal max, String rawText) {

    public boolean hasNumericRange() {
        return min != null || max != null;
    }

    static ParsedRange numeric(BigDecimal min, BigDecimal max, String raw) {
        return new ParsedRange(min, max, raw);
    }

    static ParsedRange textOnly(String raw) {
        return new ParsedRange(null, null, raw);
    }
}

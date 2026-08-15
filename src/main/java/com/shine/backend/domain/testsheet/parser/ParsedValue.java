package com.shine.backend.domain.testsheet.parser;

import com.shine.backend.domain.testitem.entity.ResultType;

import java.math.BigDecimal;

/**
 * 검사지의 결과값 한 칸을 분해한 결과.
 *
 * MIXED는 numberValue와 textValue를 동시에 갖는다.
 * "음성(0.07)" 같은 면역 검사는 판정(음성)과 측정치(0.07)가 함께 찍히는데,
 * 하나만 저장하면 판정이 사라지거나 추이를 볼 수 없다.
 */
public record ParsedValue(
        ResultType type,
        BigDecimal numberValue,
        String textValue,
        String rawValue
) {
    public static ParsedValue number(BigDecimal value, String raw) {
        return new ParsedValue(ResultType.NUMBER, value, null, raw);
    }

    public static ParsedValue text(String value, String raw) {
        return new ParsedValue(ResultType.TEXT, null, value, raw);
    }

    public static ParsedValue mixed(String text, BigDecimal number, String raw) {
        return new ParsedValue(ResultType.MIXED, number, text, raw);
    }
}

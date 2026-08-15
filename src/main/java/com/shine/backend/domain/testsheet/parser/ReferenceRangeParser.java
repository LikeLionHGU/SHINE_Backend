package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 검사지의 '참고치' 칸을 읽는다.
 *
 * ⚠️ 열거형("양성,음성,약양성")은 정상값 목록이 아니라
 *    이 검사에서 나올 수 있는 값의 목록인 경우가 많다.
 *    정상값으로 오독하면 양성인 결과를 정상으로 판정하게 되므로,
 *    숫자 범위가 아니면 텍스트로 보존만 하고 판정에는 쓰지 않는다.
 */
@Component
public class ReferenceRangeParser {

    private static final String NUM = "-?\\d+(?:\\.\\d+)?";

    /** "8 ~ 38", "4.0-10.0", "110 ∼ 450" */
    private static final Pattern RANGE = Pattern.compile(
            "^\\s*(" + NUM + ")\\s*[~∼-]\\s*(" + NUM + ")\\s*$");

    /** "< 140", "≤ 5.6", "이하 140" */
    private static final Pattern UPPER_ONLY = Pattern.compile(
            "^\\s*(?:<|≤|이하)?\\s*(" + NUM + ")\\s*(?:이하|미만)?\\s*$");

    /** "> 30", "≥ 11.0", "이상 30" */
    private static final Pattern LOWER_ONLY = Pattern.compile(
            "^\\s*(?:>|≥|이상)\\s*(" + NUM + ")\\s*$");

    public ParsedRange parse(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String value = raw.trim();

        Matcher range = RANGE.matcher(value);
        if (range.matches()) {
            return ParsedRange.numeric(
                    new BigDecimal(range.group(1)),
                    new BigDecimal(range.group(2)),
                    raw);
        }

        Matcher lower = LOWER_ONLY.matcher(value);
        if (lower.matches()) {
            return ParsedRange.numeric(new BigDecimal(lower.group(1)), null, raw);
        }

        Matcher upper = UPPER_ONLY.matcher(value);
        if (upper.matches()) {
            return ParsedRange.numeric(null, new BigDecimal(upper.group(1)), raw);
        }

        // 열거형·서술형은 보존만 한다
        return ParsedRange.textOnly(raw);
    }
}

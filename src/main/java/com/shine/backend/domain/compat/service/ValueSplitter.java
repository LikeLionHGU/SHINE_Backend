package com.shine.backend.domain.compat.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 프론트가 보낸 value 문자열을 값·단위·참고치로 쪼갠다.
 *
 *   "12.2 g/dL (11~15)"  →  값 12.2 / 단위 g/dL / 참고치 11~15
 *   "음성(0.07)"          →  값 음성(0.07) / 단위 없음 / 참고치 없음
 *   "음성"                →  값 음성
 *   "213 K/UL"            →  값 213 / 단위 K/UL
 *
 * 괄호 안이 범위(~ 또는 -)면 참고치, 단일 숫자면 측정치로 본다.
 * 측정치인 경우 원문을 그대로 넘겨 기존 ValueParser가 MIXED로 처리하게 한다.
 */
@Component
public class ValueSplitter {

    public record Split(String value, String unit, String referenceRange) {}

    private static final Pattern WITH_PARENS = Pattern.compile("^(?<head>[^()]*?)\\s*\\((?<inner>[^)]*)\\)\\s*$");
    private static final Pattern RANGE_INNER = Pattern.compile(".*[~∼]|.*\\d\\s*-\\s*\\d.*");
    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*(?<num>-?\\d+(?:\\.\\d+)?)\\s*(?<unit>.*)$");

    public Split split(String raw) {
        if (raw == null || raw.isBlank()) return new Split(null, null, null);

        String text = raw.trim();
        String referenceRange = null;

        Matcher parens = WITH_PARENS.matcher(text);
        if (parens.matches()) {
            String head = parens.group("head").trim();
            String inner = parens.group("inner").trim();

            boolean headIsNumeric = LEADING_NUMBER.matcher(head).matches();
            boolean innerIsRange = RANGE_INNER.matcher(inner).matches();

            if (headIsNumeric || innerIsRange) {
                // "12.2 g/dL (11~15)" — 괄호는 참고치다
                referenceRange = inner;
                text = head;
            }
            // 그 외("음성(0.07)")는 원문을 유지해 MIXED로 처리되게 둔다
        }

        Matcher number = LEADING_NUMBER.matcher(text);
        if (number.matches()) {
            String unit = number.group("unit").trim();
            return new Split(number.group("num"), unit.isEmpty() ? null : unit, referenceRange);
        }

        return new Split(text, null, referenceRange);
    }
}

package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 검사지 결과값 문자열을 구조화한다.
 *
 *   ① 순수 정량   "18", "12.2"
 *   ② 순수 정성   "양성", "Non Reactive"
 *   ③ 정성+정량   "음성(0.07)"        ← 괄호 안이 숫자
 *   ④ 기호형      "음성(-)", "RH(+)"  ← 괄호 안이 기호
 *   ⑤ 혈액형      "A"
 *   ⑥ 정성+한계치 "양성(>500)"        ← 괄호 안이 부등호 + 숫자
 *
 * ⑥은 측정기의 상한/하한을 넘겨 정확한 값이 없는 경우다. 예전에는 못 읽어서
 * "양성(>500)" 전체가 정성 사전에 없는 표기가 되고 판정이 막혔다(#8-3).
 * ">500"은 측정치가 아니라 "500을 넘었다"는 말이라 숫자로 저장하지 않는다.
 */
@Component
public class ValueParser {

    /** "음성(0.07)" — 괄호 안이 숫자인 경우만 */
    private static final Pattern MIXED = Pattern.compile(
            "^(?<text>[^()]+?)\\s*\\(\\s*(?<num>-?\\d+(?:\\.\\d+)?)\\s*\\)$");

    /** "양성(>500)", "음성(<0.1)", "양성(500 이상)" */
    private static final Pattern LIMITED = Pattern.compile(
            "^(?<text>[^()]+?)\\s*\\(\\s*(?:[<>≤≥]\\s*)?-?\\d+(?:\\.\\d+)?\\s*(?:이상|이하|미만|초과)?\\s*\\)$");

    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");

    public ParsedValue parse(String raw) {
        if (raw == null) return null;

        String value = raw.trim();
        if (value.isEmpty()) return null;

        // ③ 정성 + 측정치
        Matcher mixed = MIXED.matcher(value);
        if (mixed.matches()) {
            return ParsedValue.mixed(
                    mixed.group("text").trim(),
                    new BigDecimal(mixed.group("num")),
                    raw);
        }

        // ① 정량
        String numeric = value.replace(",", "");
        if (NUMBER.matcher(numeric).matches()) {
            return ParsedValue.number(new BigDecimal(numeric), raw);
        }

        // ⑥ 정성 + 한계치. 숫자는 버리고 정성 부분만 판정에 넘긴다.
        Matcher limited = LIMITED.matcher(value);
        if (limited.matches()) {
            String text = limited.group("text").trim();
            if (!text.isEmpty()) {
                return ParsedValue.text(text, raw);
            }
        }

        // ②④⑤ 나머지는 전부 텍스트. 표준값 변환은 정규화 단계에서 한다.
        return ParsedValue.text(value, raw);
    }
}

package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 검사지 결과값 문자열을 구조화한다.
 *
 * 실제 산전검사지에서 확인된 표기 5가지를 모두 처리한다.
 *   ① 순수 정량   "18", "12.2", "213"
 *   ② 순수 정성   "양성", "음성", "정상", "Non Reactive"
 *   ③ 정성+정량   "음성(0.07)"        ← 괄호 안이 숫자
 *   ④ 기호형      "음성(-)", "RH(+)"  ← 괄호 안이 기호
 *   ⑤ 혈액형      "A"
 *
 * ③과 ④를 가르는 기준은 "괄호 안이 숫자인가"다.
 * 숫자면 분해하고, 아니면 통째로 텍스트로 둔다 — 정규화는 다음 단계가 맡는다.
 */
@Component
public class ValueParser {

    /** "음성(0.07)" — 괄호 안이 숫자인 경우만 매칭된다 */
    private static final Pattern MIXED = Pattern.compile(
            "^(?<text>[^()]+?)\\s*\\(\\s*(?<num>-?\\d+(?:\\.\\d+)?)\\s*\\)$");

    /** "18", "12.2", "-0.5" */
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

        // ②④⑤ 나머지는 전부 텍스트. 표준값 변환은 정규화 단계에서 한다.
        return ParsedValue.text(value, raw);
    }
}

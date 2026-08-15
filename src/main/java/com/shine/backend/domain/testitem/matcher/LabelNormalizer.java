package com.shine.backend.domain.testitem.matcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 검사명 표기 흔들림을 흡수한다.
 *
 * 실제 검사지에는 괄호 안에 세 가지가 섞여 들어온다.
 *   동의어    혈색소(헤모글로빈)
 *   영문약어  적혈구수(RBC)
 *   검사방법  B형간염표면항원-HBs Ag(CIA)   ← 항목명이 아니다. 떼어내야 한다
 */
public final class LabelNormalizer {

    private static final Pattern PARENS = Pattern.compile("\\([^)]*\\)");
    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern NON_WORD = Pattern.compile("[^가-힣a-zA-Z0-9]");

    private LabelNormalizer() {}

    /** 비교용 키. 공백·하이픈·콤마·괄호를 모두 없애고 소문자로 맞춘다. */
    public static String key(String raw) {
        if (raw == null) return null;
        String k = NON_WORD.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);
        return k.isEmpty() ? null : k;
    }

    /**
     * 하나의 라벨에서 매칭을 시도해볼 후보들을 만든다.
     * 앞쪽일수록 신뢰도가 높다.
     */
    public static List<String> candidates(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        Set<String> result = new LinkedHashSet<>();
        String trimmed = raw.trim();

        // ① 원문 그대로
        addIfPresent(result, key(trimmed));

        // ② 괄호·대괄호를 떼어낸 형태 — 검사방법 표기(CIA 등)를 제거하기 위함
        String withoutParens = BRACKETS.matcher(PARENS.matcher(trimmed).replaceAll(" "))
                .replaceAll(" ").trim();
        addIfPresent(result, key(withoutParens));

        // ③ 괄호 안 내용만 — "적혈구수(RBC)" 에서 RBC로도 찾아본다
        var m = PARENS.matcher(trimmed);
        while (m.find()) {
            String inner = m.group().replaceAll("[()]", "");
            addIfPresent(result, key(inner));
        }

        // ④ 콤마로 나눈 조각 — "Rho,D형혈액형검사,Rh-Ir" 처럼 여러 표기가 붙어 오는 경우
        for (String piece : trimmed.split(",")) {
            String cleaned = PARENS.matcher(piece).replaceAll(" ").trim();
            if (cleaned.length() >= 2) {
                addIfPresent(result, key(cleaned));
            }
        }

        return new ArrayList<>(result);
    }

    private static void addIfPresent(Set<String> set, String key) {
        if (key != null && key.length() >= 2) set.add(key);
    }
}

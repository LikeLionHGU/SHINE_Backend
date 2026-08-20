package com.shine.backend.domain.testitem.matcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 검사명 표기 흔들림을 흡수한다.
 *
 * 괄호 안에는 세 가지가 섞여 들어온다.
 *   동의어    혈색소(헤모글로빈)
 *   영문약어  적혈구수(RBC)
 *   검사방법  B형간염표면항원-HBs Ag(CIA)   ← 항목명이 아니다. 떼어내야 한다
 *
 * 네 번째가 있다. 괄호가 '어느 검사인지를 가르는 구분어'인 경우다.
 *   WBC(/HPF)     요침사 백혈구 — 혈액 WBC가 아니다
 *   풍진항체(IgM)  최근 감염 — 면역 여부를 보는 IgG가 아니다
 *   혈소판(분포폭)  PDW — 혈소판 수가 아니다
 * 떼어내면 전혀 다른 검사에 붙는다. 단위도 기준도 달라 판정이 통째로 뒤집힌다(#8-1).
 */
public final class LabelNormalizer {

    private static final Pattern PARENS = Pattern.compile("\\([^)]*\\)");
    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern NON_WORD = Pattern.compile("[^가-힣a-zA-Z0-9]");

    /** 괄호 안에 이게 있으면 괄호를 뗀 후보를 만들지 않는다. 검사방법 표기는 넣지 않는다. */
    private static final Set<String> DISCRIMINATORS = Set.of(
            "hpf", "lpf", "현미경",
            "igm", "iga", "ige", "igg",
            "pdw", "rdw", "mpv", "분포",
            "urine", "소변", "침사"
    );

    private LabelNormalizer() {}

    /** 비교용 키. 공백·하이픈·콤마·괄호를 모두 없애고 소문자로 맞춘다. */
    public static String key(String raw) {
        if (raw == null) return null;
        String k = NON_WORD.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);
        return k.isEmpty() ? null : k;
    }

    /** 매칭을 시도해볼 후보들. 앞쪽일수록 신뢰도가 높다. */
    public static List<String> candidates(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        Set<String> result = new LinkedHashSet<>();
        String trimmed = raw.trim();

        // ① 원문 그대로
        addIfPresent(result, key(trimmed));

        // 괄호 안이 구분어면 여기서 멈춘다. 떼어낸 이름은 다른 검사의 이름이다.
        if (hasDiscriminatingParens(trimmed)) {
            return new ArrayList<>(result);
        }

        // ② 괄호·대괄호를 떼어낸 형태 — 검사방법 표기(CIA 등) 제거
        String withoutParens = BRACKETS.matcher(PARENS.matcher(trimmed).replaceAll(" "))
                .replaceAll(" ").trim();
        addIfPresent(result, key(withoutParens));

        // ③ 괄호 안 내용만 — "적혈구수(RBC)" 에서 RBC로도 찾아본다
        var m = PARENS.matcher(trimmed);
        while (m.find()) {
            String inner = m.group().replaceAll("[()]", "");
            addIfPresent(result, key(inner));
        }

        // ④ 콤마로 나눈 조각 — "Rho,D형혈액형검사,Rh-Ir"
        for (String piece : trimmed.split(",")) {
            String cleaned = PARENS.matcher(piece).replaceAll(" ").trim();
            if (cleaned.length() >= 2) {
                addIfPresent(result, key(cleaned));
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 판단은 괄호 '안'만 보고 한다. 라벨 전체를 보면
     * "바이러스항체,정밀-IgM-Rubella(진단검사의학과전문의판독)" 처럼
     * 괄호 밖에 IgM이 있는 정상 표기까지 막혀서 매칭이 안 된다.
     */
    private static boolean hasDiscriminatingParens(String label) {
        return containsDiscriminator(PARENS.matcher(label))
                || containsDiscriminator(BRACKETS.matcher(label));
    }

    private static boolean containsDiscriminator(Matcher m) {
        while (m.find()) {
            String inner = m.group().replaceAll("[\\[\\]()]", "").toLowerCase(Locale.ROOT);
            for (String token : DISCRIMINATORS) {
                if (inner.contains(token)) return true;
            }
        }
        return false;
    }

    private static void addIfPresent(Set<String> set, String key) {
        if (key != null && key.length() >= 2) set.add(key);
    }
}

package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 정성 결과를 표준값으로 통일한다.
 *
 * 같은 "음성"을 병원마다 Negative / Neg / (-) / Non Reactive 로 쓴다.
 * 통일하지 않고 카탈로그의 normal_text 와 비교하면 정상인데 이상으로 판정된다.
 *
 * 모르는 값은 null을 반환한다. 추측해서 정상이라고 하는 것보다
 * UNKNOWN으로 두고 원문을 보여주는 편이 안전하다.
 */
@Component
public class QualitativeNormalizer {

    public static final String NEGATIVE = "음성";
    public static final String WEAK_POSITIVE = "약양성";
    public static final String POSITIVE = "양성";
    public static final String NORMAL = "정상";

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        // ---- 음성 ----
        put(NEGATIVE, "음성", "-", "(-)", "음성(-)", "네거티브",
                "negative", "neg", "negative(-)", "non reactive", "nonreactive",
                "non-reactive", "nr", "비반응", "미검출", "not detected", "nd", "없음");

        // ---- 약양성 (경계) ----
        put(WEAK_POSITIVE, "약양성", "의양성", "경계", "±", "+/-", "+-",
                "weak positive", "weakpositive", "weak pos", "borderline", "equivocal",
                "trace", "미량", "흔적");

        // ---- 양성 ----
        put(POSITIVE, "양성", "+", "(+)", "양성(+)", "포지티브",
                "positive", "pos", "reactive", "검출", "detected");

        // ---- 요검사 등급 ----
        put("1+", "1+", "(1+)", "＋", "1＋", "1 +");
        put("2+", "2+", "(2+)", "2＋", "2 +", "++");
        put("3+", "3+", "(3+)", "3＋", "3 +", "+++");
        put("4+", "4+", "(4+)", "4＋", "4 +", "++++");

        // ---- 판독 소견 (영상검사 등) ----
        put(NORMAL, "정상", "normal", "이상없음", "특이소견없음", "정상소견");
        put("비활동성", "비활동성", "inactive", "비활동성 병변");
        put("미결", "미결", "미정", "pending", "indeterminate");
        put("비정상", "비정상", "abnormal", "이상", "이상소견");
    }

    private static void put(String standard, String... variants) {
        for (String v : variants) {
            MAP.put(key(v), standard);
        }
    }

    private static String key(String s) {
        // 공백·마침표를 없애고 소문자로 맞춰서 표기 흔들림을 흡수한다
        return s.replaceAll("[\\s.]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * @return 표준값. 사전에 없으면 null (판정을 UNKNOWN으로 두기 위함)
     */
    public String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return MAP.get(key(trimmed));
    }

    /** 정규화 실패 시 원문을 그대로 쓰고 싶을 때 */
    public String normalizeOrRaw(String raw) {
        String normalized = normalize(raw);
        return normalized != null ? normalized : (raw == null ? null : raw.trim());
    }
}

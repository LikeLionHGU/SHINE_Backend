package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 단위 표기를 통일한다.
 * 검사지에는 g/dl, M/UL, K/UL 처럼 대소문자와 마이크로 기호가 제각각이다.
 */
@Component
public class UnitNormalizer {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        put("g/dL", "g/dl", "g/dL", "gm/dl", "g/㎗");
        put("mg/dL", "mg/dl", "mg/dL", "mg/㎗");
        put("M/µL", "m/ul", "M/UL", "m/㎕", "10^6/ul", "x10^6/ul", "10*6/ul", "10⁶/µL");
        put("K/µL", "k/ul", "K/UL", "k/㎕", "10^3/ul", "x10^3/ul", "10*3/ul", "10³/µL");
        put("IU/L", "iu/l", "IU/L");
        put("U/L", "u/l", "U/L");
        put("mIU/mL", "miu/ml", "mIU/mL");
        // mIU/L 은 mIU/mL 의 1/1000 이다. 같은 값으로 접으면 TSH 수치가 화면에서
        // 1000배 틀린 단위를 달고 나간다. 따로 둔다.
        put("mIU/L", "miu/l", "mIU/L", "µIU/mL", "uiu/ml");
        put("ng/mL", "ng/ml", "ng/mL");
        put("pg/mL", "pg/ml", "pg/mL");
        put("µg/dL", "ug/dl", "µg/dl", "mcg/dl");
        put("%", "%");
        put("S/CO", "s/co", "S/CO", "sco");
        put("mOsm/kg", "mosm/kg");
        put("mmol/L", "mmol/l");
        put("mEq/L", "meq/l");
        put("fL", "fl");
        put("pg", "pg");
        put("/HPF", "/hpf", "개/hpf", "hpf");
        put("mm/hr", "mm/hr", "mm/h");
    }

    private static void put(String standard, String... variants) {
        for (String v : variants) {
            MAP.put(key(v), standard);
        }
    }

    /**
     * 비교용 키.
     *
     * 마이크로는 코드포인트가 셋이다 — µ(U+00B5 MICRO SIGN), μ(U+03BC 그리스 뮤),
     * 그리고 조판 문자 ㎕. 눈으로는 구분이 안 되는데 문자열로는 다르다.
     * 이걸 접지 않아서 "10^3/µL"와 "10^3/uL"가 다른 단위로 갈렸고 판정이 막혔다(#8-2).
     */
    private static String key(String s) {
        String v = s.replaceAll("\\s", "").toLowerCase(Locale.ROOT);

        v = v.replace('\u00B5', 'u')
             .replace('\u03BC', 'u')
             .replace("㎕", "ul")
             .replace("㎗", "dl")
             .replace("㎖", "ml")
             .replace("㎎", "mg")
             .replace("×", "x");

        v = v.replace("²", "^2").replace("³", "^3").replace("⁶", "^6");
        v = v.replace("*", "^");
        v = v.replace("x10^", "10^");

        return v;
    }

    /** 사전에 없으면 원문 그대로. 단위는 몰라도 값 자체는 쓸 수 있다. */
    public String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return MAP.getOrDefault(key(trimmed), trimmed);
    }
}

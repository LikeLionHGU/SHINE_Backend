package com.shine.backend.domain.testsheet.parser;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 단위 표기를 통일한다.
 *
 * 실제 검사지에는 g/dl, M/UL, K/UL 처럼 대소문자와 마이크로 기호가 제각각이다.
 * 단위가 다르면 같은 항목인데 다른 값으로 보이거나, 단위 검증이 헛돈다.
 */
@Component
public class UnitNormalizer {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        put("g/dL", "g/dl", "g/dL", "gm/dl", "g/㎗");
        put("mg/dL", "mg/dl", "mg/dL", "mg/㎗");
        put("M/µL", "m/ul", "M/UL", "m/㎕", "10^6/ul", "x10^6/ul", "10*6/ul");
        put("K/µL", "k/ul", "K/UL", "k/㎕", "10^3/ul", "x10^3/ul", "10*3/ul");
        put("IU/L", "iu/l", "IU/L");
        put("U/L", "u/l", "U/L");
        put("mIU/mL", "miu/ml", "mIU/mL", "miu/l");
        put("ng/mL", "ng/ml", "ng/mL");
        put("pg/mL", "pg/ml", "pg/mL");
        put("µg/dL", "ug/dl", "µg/dl", "mcg/dl");
        put("%", "%");
        put("S/CO", "s/co", "S/CO", "sco");
        put("mOsm/kg", "mosm/kg");
        put("mmol/L", "mmol/l");
        put("mEq/L", "meq/l");
    }

    private static void put(String standard, String... variants) {
        for (String v : variants) {
            MAP.put(key(v), standard);
        }
    }

    private static String key(String s) {
        return s.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }

    /** 사전에 없으면 원문을 그대로 돌려준다. 단위는 몰라도 값 자체는 쓸 수 있다. */
    public String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return MAP.getOrDefault(key(trimmed), trimmed);
    }
}

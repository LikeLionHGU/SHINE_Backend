package com.shine.backend.domain.testsheet.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitNormalizerTest {

    private final UnitNormalizer normalizer = new UnitNormalizer();

    @Test
    @DisplayName("실제 검사지의 단위 표기를 표준형으로 바꾼다")
    void normalizesRealSheetUnits() {
        assertThat(normalizer.normalize("g/dl")).isEqualTo("g/dL");
        assertThat(normalizer.normalize("M/UL")).isEqualTo("M/µL");
        assertThat(normalizer.normalize("K/UL")).isEqualTo("K/µL");
        assertThat(normalizer.normalize("IU/L")).isEqualTo("IU/L");
        assertThat(normalizer.normalize("S/CO")).isEqualTo("S/CO");
        assertThat(normalizer.normalize("%")).isEqualTo("%");
    }

    @Test
    @DisplayName("모르는 단위는 원문을 그대로 둔다")
    void keepsUnknownUnit() {
        assertThat(normalizer.normalize("mOsm/kg")).isEqualTo("mOsm/kg");
        assertThat(normalizer.normalize("이상한단위")).isEqualTo("이상한단위");
    }

    @Test
    @DisplayName("빈 값은 null")
    void handlesEmpty() {
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("  ")).isNull();
    }
}

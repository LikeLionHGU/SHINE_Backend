package com.shine.backend.domain.testitem.matcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelNormalizerTest {

    @Test
    @DisplayName("공백·하이픈·콤마를 무시하고 같은 키로 만든다")
    void ignoresSeparators() {
        assertThat(LabelNormalizer.key("HBs Ag")).isEqualTo(LabelNormalizer.key("HBsAg"));
        assertThat(LabelNormalizer.key("Non Reactive")).isEqualTo(LabelNormalizer.key("non-reactive"));
        assertThat(LabelNormalizer.key("Rh-Ir")).isEqualTo("rhir");
    }

    @Test
    @DisplayName("검사방법 표기가 붙어 있어도 항목명만 남긴 후보를 만든다")
    void stripsMethodSuffix() {
        // (CIA)는 화학발광면역측정법이라는 '방법'이지 항목명이 아니다
        assertThat(LabelNormalizer.candidates("B형간염표면항원-HBs Ag(CIA)"))
                .contains(LabelNormalizer.key("B형간염표면항원-HBs Ag"));
    }

    @Test
    @DisplayName("괄호 안팎을 각각 후보로 만든다")
    void splitsParentheses() {
        var candidates = LabelNormalizer.candidates("적혈구수(RBC)");

        assertThat(candidates).contains("적혈구수", "rbc");
    }

    @Test
    @DisplayName("콤마로 여러 표기가 붙어 와도 조각별로 찾는다")
    void splitsByComma() {
        var candidates = LabelNormalizer.candidates("Rho,D형혈액형검사,Rh-Ir");

        assertThat(candidates).contains("rhir");
    }

    @Test
    @DisplayName("판독 표기와 대괄호를 떼어낸다")
    void stripsBrackets() {
        assertThat(LabelNormalizer.candidates("흉부 [Chest PA](폐결핵)"))
                .contains(LabelNormalizer.key("흉부 Chest PA"));
    }

    @Test
    @DisplayName("빈 값은 후보가 없다")
    void handlesEmpty() {
        assertThat(LabelNormalizer.candidates(null)).isEmpty();
        assertThat(LabelNormalizer.candidates("   ")).isEmpty();
    }
}

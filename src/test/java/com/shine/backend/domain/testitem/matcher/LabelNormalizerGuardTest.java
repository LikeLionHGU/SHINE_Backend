package com.shine.backend.domain.testitem.matcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이름이 겹치는 다른 검사가 판정을 잡아먹던 문제(전달사항 8-1).
 * 괄호를 떼어낸 후보를 만들다 보니 요침사 WBC가 혈액 WBC로, 풍진 IgM이 IgG로 붙었다.
 */
class LabelNormalizerGuardTest {

    @Test
    @DisplayName("요침사 WBC(/HPF)를 혈액 WBC로 보지 않는다")
    void doesNotStripHpf() {
        assertThat(LabelNormalizer.candidates("WBC(/HPF)")).doesNotContain("wbc");
    }

    @Test
    @DisplayName("풍진 IgM을 IgG로 보지 않는다")
    void doesNotStripImmunoglobulinClass() {
        assertThat(LabelNormalizer.candidates("풍진항체(IgM)")).doesNotContain("풍진항체");
    }

    @Test
    @DisplayName("혈소판 분포폭(PDW)을 혈소판 수로 보지 않는다")
    void doesNotStripDistributionWidth() {
        assertThat(LabelNormalizer.candidates("혈소판(분포폭)")).doesNotContain("혈소판");
        assertThat(LabelNormalizer.candidates("Platelet(PDW)")).doesNotContain("platelet");
    }

    @Test
    @DisplayName("검사방법 표기(CIA)는 예전처럼 떼어낸다")
    void stillStripsMethodSuffix() {
        assertThat(LabelNormalizer.candidates("B형간염표면항원-HBs Ag(CIA)"))
                .contains(LabelNormalizer.key("B형간염표면항원-HBs Ag"));
    }

    @Test
    @DisplayName("괄호 밖에 있는 IgM 표기는 막지 않는다")
    void keepsMatchingWhenClassIsOutsideParens() {
        // 실제 검사지 표기. 괄호 안은 판독 주체이지 검사 구분이 아니다.
        assertThat(LabelNormalizer.candidates("바이러스항체,정밀-IgM-Rubella(진단검사의학과전문의판독)"))
                .contains(LabelNormalizer.key("바이러스항체,정밀-IgM-Rubella"));
    }

    @Test
    @DisplayName("동의어·약어 괄호는 예전처럼 후보로 만든다")
    void stillSplitsSynonyms() {
        assertThat(LabelNormalizer.candidates("적혈구수(RBC)")).contains("적혈구수", "rbc");
    }
}

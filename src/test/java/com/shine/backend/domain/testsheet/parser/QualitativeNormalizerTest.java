package com.shine.backend.domain.testsheet.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualitativeNormalizerTest {

    private final QualitativeNormalizer normalizer = new QualitativeNormalizer();

    @Test
    @DisplayName("음성의 여러 표기를 하나로 모은다")
    void normalizesNegative() {
        assertThat(normalizer.normalize("음성")).isEqualTo("음성");
        assertThat(normalizer.normalize("음성(-)")).isEqualTo("음성");
        assertThat(normalizer.normalize("Negative")).isEqualTo("음성");
        assertThat(normalizer.normalize("neg")).isEqualTo("음성");
        assertThat(normalizer.normalize("(-)")).isEqualTo("음성");
        // 실제 검사지의 매독 RPR 표기
        assertThat(normalizer.normalize("Non Reactive")).isEqualTo("음성");
        assertThat(normalizer.normalize("Non-Reactive")).isEqualTo("음성");
    }

    @Test
    @DisplayName("양성의 여러 표기를 하나로 모은다")
    void normalizesPositive() {
        assertThat(normalizer.normalize("양성")).isEqualTo("양성");
        assertThat(normalizer.normalize("Positive")).isEqualTo("양성");
        assertThat(normalizer.normalize("(+)")).isEqualTo("양성");
        assertThat(normalizer.normalize("Reactive")).isEqualTo("양성");
    }

    @Test
    @DisplayName("경계값은 약양성으로 모은다")
    void normalizesWeakPositive() {
        assertThat(normalizer.normalize("약양성")).isEqualTo("약양성");
        assertThat(normalizer.normalize("Borderline")).isEqualTo("약양성");
        assertThat(normalizer.normalize("의양성")).isEqualTo("약양성");
        assertThat(normalizer.normalize("trace")).isEqualTo("약양성");
    }

    @Test
    @DisplayName("요검사 등급 표기를 통일한다")
    void normalizesUrineGrade() {
        assertThat(normalizer.normalize("2+")).isEqualTo("2+");
        assertThat(normalizer.normalize("(2+)")).isEqualTo("2+");
        assertThat(normalizer.normalize("++")).isEqualTo("2+");
    }

    @Test
    @DisplayName("대소문자와 공백이 달라도 같은 값으로 본다")
    void ignoresCaseAndSpace() {
        assertThat(normalizer.normalize("NEGATIVE")).isEqualTo("음성");
        assertThat(normalizer.normalize("  non reactive  ")).isEqualTo("음성");
        assertThat(normalizer.normalize("Non  Reactive")).isEqualTo("음성");
    }

    @Test
    @DisplayName("판독 소견도 표준화한다")
    void normalizesRadiologyFindings() {
        assertThat(normalizer.normalize("정상")).isEqualTo("정상");
        assertThat(normalizer.normalize("비활동성")).isEqualTo("비활동성");
        assertThat(normalizer.normalize("미결")).isEqualTo("미결");
    }

    @Test
    @DisplayName("모르는 값은 null을 준다 — 추측해서 정상이라고 하지 않는다")
    void returnsNullForUnknown() {
        assertThat(normalizer.normalize("알 수 없는 값")).isNull();
        assertThat(normalizer.normalize("A")).isNull();   // 혈액형은 정상/이상 판정 대상이 아니다
        assertThat(normalizer.normalize(null)).isNull();
        assertThat(normalizer.normalize("")).isNull();
    }
}

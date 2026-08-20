package com.shine.backend.domain.testsheet.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 전달사항 8번에서 공유된 판정 버그 중 서버에도 있던 것들. */
class ParserRegressionTest {

    @Nested
    @DisplayName("단위 표기 차이 (8-2)")
    class Units {

        private final UnitNormalizer normalizer = new UnitNormalizer();

        @Test
        @DisplayName("마이크로 기호 세 가지를 같은 단위로 본다")
        void foldsMicroSign() {
            // 눈으로는 구분이 안 되는데 코드포인트는 다르다
            assertThat(normalizer.normalize("10^3/\u00B5L")).isEqualTo("K/µL");   // µ MICRO SIGN
            assertThat(normalizer.normalize("10^3/\u03BCL")).isEqualTo("K/µL");   // μ 그리스 뮤
            assertThat(normalizer.normalize("10^3/uL")).isEqualTo("K/µL");        // ASCII u
            assertThat(normalizer.normalize("K/㎕")).isEqualTo("K/µL");            // 조판 문자
        }

        @Test
        @DisplayName("지수 표기가 달라도 같은 단위로 본다")
        void foldsExponentNotation() {
            assertThat(normalizer.normalize("10³/µL")).isEqualTo("K/µL");
            assertThat(normalizer.normalize("x10^3/uL")).isEqualTo("K/µL");
            assertThat(normalizer.normalize("10*3/ul")).isEqualTo("K/µL");
            assertThat(normalizer.normalize("10^6/μL")).isEqualTo("M/µL");
        }

        @Test
        @DisplayName("mIU/L 은 mIU/mL 의 1/1000 이라 따로 둔다")
        void doesNotFoldDifferentMagnitudes() {
            assertThat(normalizer.normalize("mIU/L")).isEqualTo("mIU/L");
            assertThat(normalizer.normalize("mIU/mL")).isEqualTo("mIU/mL");
        }
    }

    @Nested
    @DisplayName("정성 결과의 괄호 표기 (8-3)")
    class Qualitative {

        private final ValueParser parser = new ValueParser();

        @Test
        @DisplayName("괄호 안이 측정치면 판정과 값을 모두 남긴다")
        void keepsBoth() {
            assertThat(parser.parse("음성(4.80)").textValue()).isEqualTo("음성");
            assertThat(parser.parse("음성(4.80)").numberValue()).isEqualByComparingTo("4.80");
        }

        @Test
        @DisplayName("괄호 안이 한계치면 판정만 남기고 숫자는 버린다")
        void keepsVerdictOnly() {
            // ">500"은 측정치가 아니라 "500을 넘었다"는 말이다. 숫자로 저장하면 추이가 거짓이 된다.
            assertThat(parser.parse("양성(>500)").textValue()).isEqualTo("양성");
            assertThat(parser.parse("양성(>500)").numberValue()).isNull();
            assertThat(parser.parse("양성(>500)").rawValue()).isEqualTo("양성(>500)");

            assertThat(parser.parse("음성(<0.1)").textValue()).isEqualTo("음성");
            assertThat(parser.parse("양성(500 이상)").textValue()).isEqualTo("양성");
        }

        @Test
        @DisplayName("기호형은 예전처럼 통째로 둔다")
        void keepsSymbolNotation() {
            assertThat(parser.parse("음성(-)").textValue()).isEqualTo("음성(-)");
            assertThat(parser.parse("RH(+)").textValue()).isEqualTo("RH(+)");
        }
    }
}

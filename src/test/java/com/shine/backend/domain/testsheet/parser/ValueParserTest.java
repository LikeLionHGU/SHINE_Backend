package com.shine.backend.domain.testsheet.parser;

import com.shine.backend.domain.testitem.entity.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 값은 전부 실제 산전검진 결과지에서 그대로 가져온 것이다.
 */
class ValueParserTest {

    private final ValueParser parser = new ValueParser();

    @Nested
    @DisplayName("정량 결과")
    class Number {

        @Test
        @DisplayName("정수와 소수를 숫자로 읽는다")
        void parsesNumbers() {
            assertThat(parser.parse("18").numberValue()).isEqualByComparingTo("18");
            assertThat(parser.parse("12.2").numberValue()).isEqualByComparingTo("12.2");
            assertThat(parser.parse("213").numberValue()).isEqualByComparingTo("213");
            assertThat(parser.parse("3.98").numberValue()).isEqualByComparingTo("3.98");
            assertThat(parser.parse("18").type()).isEqualTo(ResultType.NUMBER);
        }

        @Test
        @DisplayName("앞뒤 공백을 제거한다")
        void trims() {
            assertThat(parser.parse("  36.1  ").numberValue()).isEqualByComparingTo("36.1");
        }

        @Test
        @DisplayName("천 단위 콤마를 무시한다")
        void ignoresThousandSeparator() {
            assertThat(parser.parse("1,250").numberValue()).isEqualByComparingTo("1250");
        }
    }

    @Nested
    @DisplayName("정성 결과")
    class Text {

        @Test
        @DisplayName("한글 정성값을 텍스트로 읽는다")
        void parsesKorean() {
            assertThat(parser.parse("음성").textValue()).isEqualTo("음성");
            assertThat(parser.parse("양성").textValue()).isEqualTo("양성");
            assertThat(parser.parse("정상").textValue()).isEqualTo("정상");
            assertThat(parser.parse("음성").type()).isEqualTo(ResultType.TEXT);
        }

        @Test
        @DisplayName("영문 정성값과 혈액형도 텍스트다")
        void parsesEnglishAndBloodType() {
            assertThat(parser.parse("Non Reactive").textValue()).isEqualTo("Non Reactive");
            assertThat(parser.parse("A").textValue()).isEqualTo("A");
        }

        @Test
        @DisplayName("괄호 안이 기호면 분해하지 않고 통째로 둔다")
        void keepsSymbolNotation() {
            // 정규화 단계에서 "음성"으로 바꾼다. 파서는 구조만 본다.
            assertThat(parser.parse("음성(-)").textValue()).isEqualTo("음성(-)");
            assertThat(parser.parse("RH(+)").textValue()).isEqualTo("RH(+)");
            assertThat(parser.parse("음성(-)").type()).isEqualTo(ResultType.TEXT);
            assertThat(parser.parse("음성(-)").numberValue()).isNull();
        }
    }

    @Nested
    @DisplayName("정성 + 정량 혼합")
    class Mixed {

        @Test
        @DisplayName("괄호 안이 숫자면 판정과 측정치를 모두 남긴다")
        void keepsBoth() {
            ParsedValue result = parser.parse("음성(0.07)");

            assertThat(result.type()).isEqualTo(ResultType.MIXED);
            assertThat(result.textValue()).isEqualTo("음성");
            assertThat(result.numberValue()).isEqualByComparingTo("0.07");
        }

        @Test
        @DisplayName("HIV 검사 표기도 같은 방식으로 읽는다")
        void parsesHivNotation() {
            ParsedValue result = parser.parse("음성(0.08)");

            assertThat(result.textValue()).isEqualTo("음성");
            assertThat(result.numberValue()).isEqualByComparingTo("0.08");
        }

        @Test
        @DisplayName("양성인 경우에도 측정치를 잃지 않는다")
        void keepsValueWhenPositive() {
            ParsedValue result = parser.parse("양성(12.5)");

            assertThat(result.textValue()).isEqualTo("양성");
            assertThat(result.numberValue()).isEqualByComparingTo("12.5");
        }
    }

    @Nested
    @DisplayName("빈 값")
    class Empty {

        @Test
        @DisplayName("null과 공백은 null을 반환한다")
        void returnsNull() {
            assertThat(parser.parse(null)).isNull();
            assertThat(parser.parse("")).isNull();
            assertThat(parser.parse("   ")).isNull();
        }
    }

    @Test
    @DisplayName("원문은 항상 그대로 보존한다")
    void preservesRawValue() {
        assertThat(parser.parse("  음성(0.07)  ").rawValue()).isEqualTo("  음성(0.07)  ");
        assertThat(parser.parse("12.2").rawValue()).isEqualTo("12.2");
    }
}

package com.shine.backend.domain.testsheet.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceRangeParserTest {

    private final ReferenceRangeParser parser = new ReferenceRangeParser();

    @Test
    @DisplayName("실제 검사지의 숫자 범위를 읽는다")
    void parsesNumericRange() {
        assertThat(parser.parse("8 ~ 38").min()).isEqualByComparingTo("8");
        assertThat(parser.parse("8 ~ 38").max()).isEqualByComparingTo("38");
        assertThat(parser.parse("4.0 ~ 10.0").max()).isEqualByComparingTo("10.0");
        assertThat(parser.parse("110 ~ 450").max()).isEqualByComparingTo("450");
        assertThat(parser.parse("35 - 54").min()).isEqualByComparingTo("35");
    }

    @Test
    @DisplayName("한쪽만 있는 기준도 읽는다")
    void parsesOpenEnded() {
        assertThat(parser.parse("< 140").max()).isEqualByComparingTo("140");
        assertThat(parser.parse("≥ 11.0").min()).isEqualByComparingTo("11.0");
        assertThat(parser.parse("140 이하").max()).isEqualByComparingTo("140");
    }

    @Test
    @DisplayName("열거형은 숫자로 해석하지 않고 원문만 보존한다")
    void keepsEnumerationAsText() {
        // "양성,음성,약양성"은 정상값 목록이 아니라 나올 수 있는 값 목록이다.
        // 정상값으로 쓰면 양성인 결과를 정상으로 판정하게 된다.
        ParsedRange result = parser.parse("양성,음성,약양성");

        assertThat(result.hasNumericRange()).isFalse();
        assertThat(result.rawText()).isEqualTo("양성,음성,약양성");
    }

    @Test
    @DisplayName("혈액형과 판독 소견 목록도 텍스트로 둔다")
    void keepsOtherEnumerations() {
        assertThat(parser.parse("A,B,A,O,기결과").hasNumericRange()).isFalse();
        assertThat(parser.parse("비활동성,정상,미검").hasNumericRange()).isFalse();
        assertThat(parser.parse("Non Reactive,미검").hasNumericRange()).isFalse();
    }

    @Test
    @DisplayName("경계값이 섞인 복합 표기도 숫자로 해석하지 않는다")
    void keepsMixedNotationAsText() {
        // "Borderline( 8~12 ), 양성,음성,약양성"
        // 8~12는 경계값 구간이지 정상 범위가 아니다. 잘못 쓰면 판정이 뒤집힌다.
        ParsedRange result = parser.parse("Borderline( 8~12 ), 양성,음성,약양성");

        assertThat(result.hasNumericRange()).isFalse();
    }

    @Test
    @DisplayName("빈 값은 null")
    void handlesEmpty() {
        assertThat(parser.parse(null)).isNull();
        assertThat(parser.parse("  ")).isNull();
    }
}

package com.shine.backend.domain.testsheet.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultStatusTest {

    @Test
    @DisplayName("화면에 나가는 상태는 네 가지뿐이다")
    void hasFourLabels() {
        assertThat(ResultStatus.NORMAL.label()).isEqualTo("안심");
        assertThat(ResultStatus.CAUTION.label()).isEqualTo("주의");
        assertThat(ResultStatus.DANGER.label()).isEqualTo("위험");
        assertThat(ResultStatus.UNKNOWN.label()).isEqualTo("확인 필요");
    }

    @Test
    @DisplayName("판정하지 못한 항목을 안심으로 내려보내지 않는다")
    void neverCallsUnknownSafe() {
        // 이상 수치를 정상이라 말하는 것이 이 앱에서 가장 위험한 실패다
        assertThat(ResultStatus.UNKNOWN.label()).isNotEqualTo("안심");
    }

    @Test
    @DisplayName("엔진 상태를 프론트 bridge.ts 와 같은 규칙으로 접는다")
    void foldsEngineStatus() {
        assertThat(ResultStatus.fromEngineStatus("safe")).isEqualTo(ResultStatus.NORMAL);
        assertThat(ResultStatus.fromEngineStatus("watch")).isEqualTo(ResultStatus.CAUTION);
        assertThat(ResultStatus.fromEngineStatus("recheck")).isEqualTo(ResultStatus.CAUTION);
        assertThat(ResultStatus.fromEngineStatus("alert")).isEqualTo(ResultStatus.DANGER);
        assertThat(ResultStatus.fromEngineStatus("indeterminate")).isEqualTo(ResultStatus.UNKNOWN);
        assertThat(ResultStatus.fromEngineStatus("info_only")).isEqualTo(ResultStatus.UNKNOWN);
        assertThat(ResultStatus.fromEngineStatus("unsupported")).isEqualTo(ResultStatus.UNKNOWN);
    }

    @Test
    @DisplayName("모르는 값이면 null — 서버 판정을 그대로 쓰게 둔다")
    void returnsNullForUnknownEngineStatus() {
        assertThat(ResultStatus.fromEngineStatus(null)).isNull();
        assertThat(ResultStatus.fromEngineStatus("  ")).isNull();
        assertThat(ResultStatus.fromEngineStatus("나중에생길상태")).isNull();
    }
}

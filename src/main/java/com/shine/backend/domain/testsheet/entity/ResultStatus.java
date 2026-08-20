package com.shine.backend.domain.testsheet.entity;

import java.util.Locale;

/**
 * 항목별 판정. 코드가 결정론적으로 계산하며 AI가 만들지 않는다.
 * 애매하면 항상 이상 쪽으로 기운다 — 이상 수치를 정상이라 말하는 것이 가장 위험한 실패다.
 */
public enum ResultStatus {
    /** 안심 (초록) */
    NORMAL,
    /** 주의 (노랑) — 정상 범위를 살짝 벗어남 */
    CAUTION,
    /** 위험 (빨강) */
    DANGER,
    /** 회색 — 미매칭이거나 판정 기준이 없는 항목. 화면에는 "확인 필요"로 나간다 */
    UNKNOWN;

    /**
     * 화면에 나가는 상태 문자열. 프론트가 아는 값은 이 넷뿐이다.
     * UNKNOWN을 "안심"으로 내리면 판정하지 못한 항목이 정상인 것처럼 보인다 —
     * 이 앱에서 가장 위험한 실패다.
     */
    public String label() {
        return switch (this) {
            case NORMAL -> "안심";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> "확인 필요";
        };
    }

    /**
     * 프론트 판정 엔진의 세분화 상태를 서버 판정으로 접는다.
     * 접는 규칙은 프론트 bridge.ts 와 같아야 한다. 다르면 업로드 직후 화면과
     * 기록 탭에 다른 상태가 뜬다.
     *
     * @return 모르는 값이면 null — 호출부가 서버 판정을 그대로 쓰게 둔다
     */
    public static ResultStatus fromEngineStatus(String engineStatus) {
        if (engineStatus == null || engineStatus.isBlank()) return null;

        return switch (engineStatus.trim().toLowerCase(Locale.ROOT)) {
            case "safe" -> NORMAL;
            case "watch", "recheck" -> CAUTION;
            case "alert" -> DANGER;
            case "indeterminate", "info_only", "unsupported" -> UNKNOWN;
            default -> null;
        };
    }
}

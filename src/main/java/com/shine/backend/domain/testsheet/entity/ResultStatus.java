package com.shine.backend.domain.testsheet.entity;

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
    /** 회색 — 미매칭 항목. 원문만 표시하고 AI를 호출하지 않는다 */
    UNKNOWN
}

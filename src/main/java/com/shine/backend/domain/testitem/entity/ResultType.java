package com.shine.backend.domain.testitem.entity;

/** 검사 결과의 표기 형식. */
public enum ResultType {
    /** 정량 — 12.2 */
    NUMBER,
    /** 정성 — 음성, 양성, A형 */
    TEXT,
    /** 혼합 — "음성(0.07)" 처럼 판정과 측정치가 함께 찍히는 경우 (HBsAg, HIV 등) */
    MIXED
}

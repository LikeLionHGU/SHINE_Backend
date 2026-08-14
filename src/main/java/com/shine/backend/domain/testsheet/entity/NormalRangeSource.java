package com.shine.backend.domain.testsheet.entity;

/** 판정에 실제로 사용한 참고치의 출처. */
public enum NormalRangeSource {
    /** 검사지에 인쇄된 참고치 */
    SHEET,
    /** 카탈로그 기본값. 임신 특이 항목은 항상 이쪽을 쓴다 */
    CATALOG,
    NONE
}

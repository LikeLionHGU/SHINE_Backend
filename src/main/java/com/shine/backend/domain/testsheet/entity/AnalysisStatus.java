package com.shine.backend.domain.testsheet.entity;

/** 검사지 분석 진행 상태. 클라이언트는 이 값을 2초 간격으로 폴링한다. */
public enum AnalysisStatus {
    WAITING,
    ANALYZING,
    DONE,
    FAILED
}

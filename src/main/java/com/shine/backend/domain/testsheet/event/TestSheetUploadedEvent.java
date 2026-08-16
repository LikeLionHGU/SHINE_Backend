package com.shine.backend.domain.testsheet.event;

/** 검사지가 저장된 뒤 발행된다. 커밋이 끝난 다음에만 분석이 시작되도록 하기 위한 신호. */
public record TestSheetUploadedEvent(Long testSheetId) {}

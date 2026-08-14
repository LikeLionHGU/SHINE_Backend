package com.shine.backend.domain.appointment.entity;

/** 일정을 누가 만들었는지. SYSTEM 일정만 주차 변경 시 재생성 대상이 된다. */
public enum ScheduleSource {
    /** 가입 시 주차별로 자동 생성 */
    SYSTEM,
    /** 사용자가 직접 등록하거나, 자동 생성분을 수정한 경우 */
    USER
}

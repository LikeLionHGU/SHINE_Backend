package com.shine.backend.domain.appointment.entity;

import com.shine.backend.domain.user.entity.User;
import com.shine.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 병원 방문 일정. 가입 시 prenatal_schedule.json 기반으로 자동 생성된다. */
@Getter
@Entity
@Builder
@Table(name = "appointments")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 100)
    private String location;

    @Column(name = "visit_at", nullable = false)
    private LocalDateTime visitAt;

    @Column(name = "pregnancy_week", nullable = false)
    private Integer pregnancyWeek;

    /** 캘린더 마커 색을 가른다 — 빨간 점(산부인과) vs 빈 원(기타). */
    @Column(name = "is_obgyn", nullable = false)
    private boolean obgyn;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_status", nullable = false)
    private VisitStatus visitStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false)
    private ScheduleSource createdBy;

    @Column(name = "schedule_code", length = 30)
    private String scheduleCode;

    /**
     * 자동 생성 일정을 사용자가 수정하면 USER로 바뀌어,
     * 이후 주차 변경 시 재생성 대상에서 제외된다.
     */
    public void update(String title, String location, LocalDateTime visitAt,
                       Boolean obgyn, VisitStatus visitStatus, Integer pregnancyWeek) {
        if (title != null) this.title = title;
        if (location != null) this.location = location;
        if (visitAt != null) this.visitAt = visitAt;
        if (obgyn != null) this.obgyn = obgyn;
        if (visitStatus != null) this.visitStatus = visitStatus;
        if (pregnancyWeek != null) this.pregnancyWeek = pregnancyWeek;
        this.createdBy = ScheduleSource.USER;
    }
}

package com.shine.backend.domain.question.entity;

import com.shine.backend.domain.appointment.entity.Appointment;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.user.entity.User;
import com.shine.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 진료 때 물어볼 질문 메모.
 * AI 챗봇이 아니다 — AI가 의학적 질문에 직접 답하면 상담 행위로 해석될 수 있다.
 * createdBy=AI는 "질문 문구를 추천했다"는 뜻이지 답변이 아니다.
 */
@Getter
@Entity
@Builder
@Table(name = "questions")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_sheet_id")
    private TestSheet testSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false)
    private QuestionSource createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_status", nullable = false)
    private QuestionStatus questionStatus;

    @Column(name = "doctor_answer", length = 1000)
    private String doctorAnswer;

    /** 3초 닥터 브리핑 / 보호자 공유 메일에 포함할지 */
    @Column(name = "include_in_briefing", nullable = false)
    private boolean includeInBriefing;

    public void answer(String doctorAnswer) {
        this.doctorAnswer = doctorAnswer;
        this.questionStatus = QuestionStatus.ANSWERED;
    }

    public void update(String doctorAnswer, QuestionStatus status, Boolean includeInBriefing) {
        if (doctorAnswer != null) this.doctorAnswer = doctorAnswer;
        if (status != null) this.questionStatus = status;
        if (includeInBriefing != null) this.includeInBriefing = includeInBriefing;
    }
}

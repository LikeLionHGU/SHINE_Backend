package com.shine.backend.domain.user.entity;

import com.shine.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 20, unique = true)
    private String loginId;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 30)
    private String nickname;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "guardian_email", length = 100)
    private String guardianEmail;

    @Column(name = "additional_email", length = 100)
    private String additionalEmail;

    /** 설계결정①: 주수는 저장하지 않는다. 이 값 하나로 매번 계산한다. */
    @Column(name = "last_period_date", nullable = false)
    private LocalDate lastPeriodDate;

    @Column(name = "camera_agreed", nullable = false)
    private boolean cameraAgreed;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "terms_agreed_at", nullable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_agreed_at", nullable = false)
    private LocalDateTime privacyAgreedAt;

    /** 건강정보는 민감정보라 일반 개인정보 동의와 별도로 받아야 한다. */
    @Column(name = "sensitive_agreed_at", nullable = false)
    private LocalDateTime sensitiveAgreedAt;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // ---------- 주수 계산 ----------

    /** 회원가입에서 받은 주차를 최종월경일로 역산한다. */
    public static LocalDate toLastPeriodDate(int pregnancyWeek, LocalDate today) {
        return today.minusWeeks(pregnancyWeek - 1L);
    }

    public int getPregnancyWeek(LocalDate today) {
        return (int) (ChronoUnit.DAYS.between(lastPeriodDate, today) / 7) + 1;
    }

    public int getPregnancyDay(LocalDate today) {
        return (int) (ChronoUnit.DAYS.between(lastPeriodDate, today) % 7);
    }

    /** 출산예정일 = 최종월경일 + 280일 (Naegele). 계산값이며 저장하지 않는다. */
    public LocalDate getDueDate() {
        return lastPeriodDate.plusDays(280);
    }

    // ---------- 변경 ----------

    public void updateProfile(String name, String nickname, String phoneNumber, String email) {
        if (name != null) this.name = name;
        if (nickname != null) this.nickname = nickname;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (email != null) this.email = email;
    }

    /**
     * 보호자·추가 이메일은 지울 수 있는 칸이다.
     *   null  → 안 보낸 것. 그대로 둔다
     *   ""    → 지운다
     *
     * 예전에는 null을 그대로 대입해서, 이름만 바꾸려고 PATCH를 보내도
     * 등록해둔 보호자 이메일이 함께 지워졌다(전달사항 5번).
     */
    public void updateEmails(String guardianEmail, String additionalEmail) {
        if (guardianEmail != null) this.guardianEmail = blankToNull(guardianEmail);
        if (additionalEmail != null) this.additionalEmail = blankToNull(additionalEmail);
    }

    private static String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public void updateSettings(Boolean cameraAgreed, Boolean notificationEnabled) {
        if (cameraAgreed != null) this.cameraAgreed = cameraAgreed;
        if (notificationEnabled != null) this.notificationEnabled = notificationEnabled;
    }

    public void updateLastPeriodDate(LocalDate lastPeriodDate) {
        this.lastPeriodDate = lastPeriodDate;
    }

    // ---------- 로그인 시도 제한 ----------

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** 5회 연속 실패하면 10분 잠근다. */
    public void loginFailed(LocalDateTime now) {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.lockedUntil = now.plusMinutes(10);
            this.loginFailCount = 0;
        }
    }

    public void loginSucceeded() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }
}

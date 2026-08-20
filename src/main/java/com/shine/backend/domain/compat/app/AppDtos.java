package com.shine.backend.domain.compat.app;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 프론트(SHINE_Front)의 src/lib/api/types.ts 및 src/lib/report.ts 와
 * 같은 모양으로 맞춘 응답들.
 *
 * 화면 코드를 건드리지 않고 목 데이터만 교체할 수 있게 하는 것이 목적이라
 * 필드명·형식(날짜 "YY.MM.DD", 시간 meridiem/hour/minute)을 프론트에 맞췄다.
 */
public final class AppDtos {

    private AppDtos() {}

    /** types.ts UserProfile */
    public record UserProfile(
            String name,
            String accountName,
            String phone,
            String email,
            String guardianEmail,
            String extraEmail
    ) {}

    /**
     * 개인정보 수정 요청. 부분 수정이라 보낸 칸만 바뀐다.
     * 빈 문자열은 "지운다"는 뜻이고, 아예 안 보내면 그대로 둔다.
     *
     * 프론트는 추가 이메일을 extraEmail 로, 서버는 additionalEmail 로 부른다.
     * 어느 이름으로 보내든 받도록 별칭을 걸어둔다 — 이름 하나 때문에 저장이
     * 조용히 실패하는 것이 가장 찾기 어려운 버그다.
     */
    public record UserProfileUpdate(
            @Size(min = 2, max = 20, message = "이름은 2~20자로 입력해주세요.")
            String name,

            @JsonAlias("phoneNumber")
            @Pattern(regexp = "^[+0-9][0-9 -]{7,19}$", message = "휴대폰 번호 형식이 올바르지 않아요.")
            String phone,

            @Email(message = "이메일 형식이 올바르지 않아요.")
            String email,

            @Email(message = "보호자 이메일 형식이 올바르지 않아요.")
            String guardianEmail,

            @JsonAlias("additionalEmail")
            @Email(message = "이메일 형식이 올바르지 않아요.")
            String extraEmail
    ) {}

    /** report.ts RecordEntry — 기록 탭 */
    public record RecordEntry(
            String id,
            String date,
            String week,
            String summary
    ) {}

    /** report.ts TrendPoint */
    public record TrendPoint(String date, double value) {}

    /** report.ts TrendIndicator — 분석 탭 리스트 + 상세 */
    public record TrendIndicator(
            String id,
            String title,
            String status,
            String unit,
            String definition,
            String trendSummary,
            /** 차트 y축 [하단, 상단] */
            List<Double> range,
            /** 위→아래 순서 */
            List<String> zoneLabels,
            List<TrendPoint> history
    ) {}

    /** types.ts CalendarVisit */
    public record CalendarVisit(
            String id,
            String date,
            String title,
            String place,
            String meridiem,
            int hour,
            int minute,
            boolean isHospital,
            List<String> questions
    ) {}

    /** types.ts CalendarMonthMarks */
    public record CalendarMonthMarks(
            Map<Integer, String> marks,
            Map<Integer, String> labels
    ) {}

    /** types.ts Report */
    public record Report(String date, String url) {}

    /** types.ts VisitDetail */
    public record VisitDetail(
            Report todayReport,
            Report previousReport,
            List<String> suggestedQuestions,
            List<String> questions
    ) {}
}

package com.shine.backend.domain.compat.app;

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

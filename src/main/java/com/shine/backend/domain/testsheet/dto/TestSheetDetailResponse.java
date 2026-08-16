package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** 검사지 분석 결과 화면(번역 화면). 기록 탭에서 과거 검사지를 열 때도 같은 응답을 쓴다. */
public record TestSheetDetailResponse(
        Long testSheetId,
        LocalDate testDate,
        boolean testDateConfirmed,
        int pregnancyWeek,
        String hospitalName,
        AnalysisStatus analysisStatus,
        List<Image> images,
        Summary summary,
        Counts counts,
        List<TestResultResponse> results
) {
    public record Image(int page, String imageUrl) {}

    public record Summary(String summaryForMom, String llmModel, String promptVersion) {}

    public record Counts(long danger, long caution, long normal, long unknown, int total) {}

    public static TestSheetDetailResponse of(TestSheet sheet, List<TestResult> results) {
        // 위험한 것부터 보여준다
        List<TestResultResponse> sorted = results.stream()
                .sorted(Comparator
                        .comparingInt((TestResult r) -> severity(r.getResultStatus()))
                        .thenComparing(r -> r.getTestItem() == null ? 999
                                : r.getTestItem().getDisplayOrder()))
                .map(TestResultResponse::from)
                .toList();

        return new TestSheetDetailResponse(
                sheet.getId(),
                sheet.getTestDate(),
                sheet.isTestDateConfirmed(),
                sheet.getPregnancyWeek(),
                sheet.getHospitalName(),
                sheet.getAnalysisStatus(),
                buildImages(sheet),
                new Summary(sheet.getSummaryForMom(), sheet.getLlmModel(), sheet.getPromptVersion()),
                count(results),
                sorted);
    }

    private static List<Image> buildImages(TestSheet sheet) {
        List<String> keys = sheet.getImageKeys();
        if (keys == null) return List.of();

        return IntStream.range(0, keys.size())
                .mapToObj(i -> new Image(i + 1,
                        "/api/v1/test-sheets/%d/images/%d".formatted(sheet.getId(), i + 1)))
                .toList();
    }

    private static Counts count(List<TestResult> results) {
        return new Counts(
                results.stream().filter(r -> r.getResultStatus() == ResultStatus.DANGER).count(),
                results.stream().filter(r -> r.getResultStatus() == ResultStatus.CAUTION).count(),
                results.stream().filter(r -> r.getResultStatus() == ResultStatus.NORMAL).count(),
                results.stream().filter(r -> r.getResultStatus() == ResultStatus.UNKNOWN).count(),
                results.size());
    }

    private static int severity(ResultStatus status) {
        return switch (status) {
            case DANGER -> 0;
            case CAUTION -> 1;
            case NORMAL -> 2;
            case UNKNOWN -> 3;
        };
    }
}

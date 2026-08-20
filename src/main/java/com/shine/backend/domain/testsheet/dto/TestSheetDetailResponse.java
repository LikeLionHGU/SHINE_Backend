package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.compat.service.EngineMetaCodec;
import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;
import com.shine.backend.domain.testsheet.entity.TestSheet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
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
        List<TestResultResponse> results,
        List<Food> foods,
        List<String> questions
) {
    public record Image(int page, String imageUrl) {}

    /** 업로드 때 저장해둔 추천 재료. 기록 탭에서 다시 열어도 같은 게 보여야 한다. */
    public record Food(String name, String reason) {}

    public record Summary(String summaryForMom, String llmModel, String promptVersion) {}

    public record Counts(long danger, long caution, long normal, long unknown, int total) {}

    public static TestSheetDetailResponse of(TestSheet sheet, List<TestResult> results) {
        return of(sheet, results, List.of(), List.of(), r -> null);
    }

    public static TestSheetDetailResponse of(TestSheet sheet, List<TestResult> results,
                                             List<Food> foods, List<String> questions) {
        return of(sheet, results, foods, questions, r -> null);
    }

    /**
     * @param engineMeta 업로드 때 저장해둔 판정 근거를 풀어주는 함수.
     *                   근거를 못 읽어도 검사지는 열려야 하므로 null 을 돌려줘도 된다.
     */
    public static TestSheetDetailResponse of(TestSheet sheet, List<TestResult> results,
                                             List<Food> foods, List<String> questions,
                                             Function<TestResult, EngineMetaCodec.EngineMeta> engineMeta) {
        // 위험한 것부터 보여준다
        List<TestResultResponse> sorted = results.stream()
                .sorted(Comparator
                        .comparingInt((TestResult r) -> severity(r.getResultStatus()))
                        .thenComparing(r -> r.getTestItem() == null ? 999
                                : r.getTestItem().getDisplayOrder()))
                .map(r -> TestResultResponse.from(r, engineMeta.apply(r)))
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
                sorted,
                foods == null ? List.of() : foods,
                questions == null ? List.of() : questions);
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

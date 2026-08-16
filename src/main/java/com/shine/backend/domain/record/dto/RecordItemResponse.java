package com.shine.backend.domain.record.dto;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 기록 탭의 카드 한 장. */
public record RecordItemResponse(
        Long testSheetId,
        LocalDate testDate,
        String displayDate,
        int pregnancyWeek,
        String weekLabel,
        String summaryPreview,
        AnalysisStatus analysisStatus,
        long danger,
        long caution
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yy.MM.dd");

    public static RecordItemResponse of(TestSheet sheet, long danger, long caution) {
        return new RecordItemResponse(
                sheet.getId(),
                sheet.getTestDate(),
                sheet.getTestDate().format(FMT),
                sheet.getPregnancyWeek(),
                sheet.getPregnancyWeek() + "주차",
                preview(sheet, danger, caution),
                sheet.getAnalysisStatus(),
                danger,
                caution);
    }

    /**
     * AI 요약이 아직 없으면 판정 개수로 문장을 만든다.
     * 고정 템플릿을 먼저 만들고 AI를 그 위에 얹는다 — 반대로 하면 AI 장애 시 화면이 빈다.
     */
    private static String preview(TestSheet sheet, long danger, long caution) {
        if (sheet.getAnalysisStatus() == AnalysisStatus.WAITING
                || sheet.getAnalysisStatus() == AnalysisStatus.ANALYZING) {
            return "분석 중이에요";
        }
        if (sheet.getAnalysisStatus() == AnalysisStatus.FAILED) {
            return "분석하지 못했어요";
        }
        if (sheet.getSummaryForMom() != null && !sheet.getSummaryForMom().isBlank()) {
            return sheet.getSummaryForMom();
        }
        if (danger > 0) {
            return "확인이 필요한 항목이 %d개 있어요. 선생님과 이야기해 보세요.".formatted(danger);
        }
        if (caution > 0) {
            return "주의해서 볼 항목이 %d개 있어요.".formatted(caution);
        }
        return "이번 검사에서는 모든 항목이 정상 범위 안에 있어요.";
    }
}

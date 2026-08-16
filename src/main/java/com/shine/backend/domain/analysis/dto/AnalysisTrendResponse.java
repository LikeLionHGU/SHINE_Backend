package com.shine.backend.domain.analysis.dto;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 분석 상세 — 항목 하나의 시간별 추이. */
public record AnalysisTrendResponse(
        Long testItemId,
        String itemName,
        String itemNameEn,
        String unit,
        ResultType resultType,
        String description,
        Latest latest,
        NormalRange normalRange,
        List<TrendPoint> trend,
        /** 추이 설명. 지금은 코드가 만든 고정 문장이며, 나중에 AI 문장이 이 자리에 들어간다 */
        String trendDescription
) {
    public record Latest(
            BigDecimal numberValue,
            String textValue,
            ResultStatus resultStatus,
            String statusLabel,
            LocalDate testDate,
            int pregnancyWeek
    ) {}

    /** 화면의 높음/안정/낮음 구간을 그리려면 실제 범위가 필요하다 */
    public record NormalRange(BigDecimal min, BigDecimal max, NormalRangeSource source) {}

    public record TrendPoint(
            Long testSheetId,
            LocalDate testDate,
            String displayDate,
            int pregnancyWeek,
            BigDecimal numberValue,
            String textValue,
            ResultStatus resultStatus
    ) {}
}

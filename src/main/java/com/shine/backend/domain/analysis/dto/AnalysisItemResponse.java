package com.shine.backend.domain.analysis.dto;

import com.shine.backend.domain.testsheet.entity.ResultStatus;

import java.math.BigDecimal;
import java.util.List;

/** 분석 탭의 항목 한 줄. */
public record AnalysisItemResponse(
        Long testItemId,
        Long testResultId,
        String itemName,
        ResultStatus resultStatus,
        String statusLabel,
        BigDecimal numberValue,
        String textValue,
        String unit,
        /**
         * 리스트의 작은 꺾은선용. 최근 최대 6개 값.
         * 정성 항목이거나 측정이 1회뿐이면 null — 점 하나로는 선을 그릴 수 없다.
         */
        List<BigDecimal> sparkline,
        boolean hasTrend
) {}

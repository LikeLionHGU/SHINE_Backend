package com.shine.backend.domain.testsheet.evaluator;

import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.parser.ParsedValue;

import java.math.BigDecimal;

/**
 * @param item            매칭된 카탈로그 항목. null이면 미매칭
 * @param sheetNormalMin  검사지에 인쇄된 참고치 하한
 * @param sheetVerdict    검사지에 이미 인쇄된 판정 ("정상", "미결" 등)
 */
public record EvaluationInput(
        ParsedValue value,
        TestItemCatalog item,
        BigDecimal sheetNormalMin,
        BigDecimal sheetNormalMax,
        String sheetVerdict
) {}

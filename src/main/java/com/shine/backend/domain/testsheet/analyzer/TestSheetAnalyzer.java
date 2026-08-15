package com.shine.backend.domain.testsheet.analyzer;

import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testitem.matcher.TestItemMatcher;
import com.shine.backend.domain.testsheet.evaluator.EvaluationInput;
import com.shine.backend.domain.testsheet.evaluator.EvaluationResult;
import com.shine.backend.domain.testsheet.evaluator.ResultEvaluator;
import com.shine.backend.domain.testsheet.ocr.OcrResult;
import com.shine.backend.domain.testsheet.ocr.OcrRow;
import com.shine.backend.domain.testsheet.parser.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OCR 결과를 판정까지 끝난 행들로 바꾼다.
 *
 * 순서: 항목 매칭 → 값 파싱 → 참고치 파싱 → 단위·정성값 정규화 → 판정
 * 이 단계까지는 AI가 전혀 개입하지 않는다. 전부 결정론적이라 테스트할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestSheetAnalyzer {

    private final TestItemMatcher matcher;
    private final ValueParser valueParser;
    private final ReferenceRangeParser rangeParser;
    private final UnitNormalizer unitNormalizer;
    private final QualitativeNormalizer qualitativeNormalizer;
    private final ResultEvaluator evaluator;

    public List<AnalyzedRow> analyze(OcrResult ocr) {
        List<AnalyzedRow> rows = ocr.rows().stream().map(this::analyzeRow).toList();

        long matched = rows.stream().filter(AnalyzedRow::isMatched).count();
        log.info("검사지 해석 완료 — 전체 {}행, 매칭 {}행 ({}%)",
                rows.size(), matched,
                rows.isEmpty() ? 0 : matched * 100 / rows.size());

        return rows;
    }

    private AnalyzedRow analyzeRow(OcrRow row) {
        TestItemCatalog item = matcher.match(row.label()).orElse(null);
        ParsedValue value = valueParser.parse(row.value());
        ParsedRange range = rangeParser.parse(row.referenceRange());

        if (item == null) {
            log.debug("미매칭 라벨 '{}'", row.label());
        }

        // 검사지 참고치는 숫자 범위일 때만 판정에 쓴다.
        // 열거형("양성,음성,약양성")은 정상값 목록이 아니라서 그대로 믿으면 판정이 뒤집힌다.
        boolean numericRange = range != null && range.hasNumericRange();

        EvaluationResult evaluation = evaluator.evaluate(new EvaluationInput(
                value,
                item,
                numericRange ? range.min() : null,
                numericRange ? range.max() : null,
                row.verdict()));

        // 정성값은 표준형으로 바꿔 저장한다. 원문은 rawValue에 그대로 남는다.
        String normalizedText = value == null ? null
                : qualitativeNormalizer.normalizeOrRaw(value.textValue());

        return new AnalyzedRow(
                item,
                row.label(),
                row.category(),
                row.value(),
                value == null ? null : value.type(),
                value == null ? null : value.numberValue(),
                normalizedText,
                unitNormalizer.normalize(row.unit()),
                row.unit(),
                numericRange ? range.min() : null,
                numericRange ? range.max() : null,
                range == null ? null : range.rawText(),
                evaluation.source(),
                evaluation.status(),
                row.verdict(),
                evaluation.verdictMismatch());
    }
}

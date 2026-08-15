package com.shine.backend.domain.testsheet.analyzer;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;

import java.math.BigDecimal;

/**
 * 검사지 한 줄을 해석한 결과. 그대로 TestResult 엔티티가 된다.
 *
 * item이 null이면 미매칭이다. 이때도 ocrLabel과 rawValue는 남기므로
 * 나중에 name_variants를 보강한 뒤 재파싱해서 복구할 수 있다(설계결정⑤).
 */
public record AnalyzedRow(
        TestItemCatalog item,
        String ocrLabel,
        String ocrCategory,
        String rawValue,
        ResultType resultType,
        BigDecimal numberValue,
        String textValue,
        String unit,
        String unitRaw,
        BigDecimal sheetNormalMin,
        BigDecimal sheetNormalMax,
        String sheetNormalText,
        NormalRangeSource normalRangeSource,
        ResultStatus resultStatus,
        String sheetVerdict,
        boolean verdictMismatch
) {
    public boolean isMatched() {
        return item != null;
    }
}

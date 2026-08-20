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

    /**
     * 판정만 갈아끼운 사본.
     *
     * 프론트 판정 엔진이 이미 판정을 내린 항목은 그 판정을 저장한다. 화면에는 엔진
     * 판정이 뜨는데 DB에는 서버 판정이 들어가면, 업로드 직후 "안심"이던 항목이
     * 기록 탭에서 다른 상태로 바뀐다. 사용자 눈에는 데이터가 망가진 것으로 보인다.
     */
    public AnalyzedRow withResultStatus(ResultStatus status) {
        if (status == null || status == resultStatus) return this;

        return new AnalyzedRow(
                item, ocrLabel, ocrCategory, rawValue, resultType, numberValue, textValue,
                unit, unitRaw, sheetNormalMin, sheetNormalMax, sheetNormalText,
                normalRangeSource, status, sheetVerdict, verdictMismatch);
    }
}

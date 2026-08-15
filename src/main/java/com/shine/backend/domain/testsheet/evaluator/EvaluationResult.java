package com.shine.backend.domain.testsheet.evaluator;

import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;

public record EvaluationResult(
        ResultStatus status,
        NormalRangeSource source,
        /** 검사지 판정과 우리 판정이 어긋났는지. 관리자가 모아 보면 데이터 품질 문제가 드러난다 */
        boolean verdictMismatch
) {
    static EvaluationResult unknown() {
        return new EvaluationResult(ResultStatus.UNKNOWN, NormalRangeSource.NONE, false);
    }
}

package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;

import java.math.BigDecimal;

public record TestResultResponse(
        Long testResultId,
        Long testItemId,
        String itemName,
        String itemNameEn,
        /** 검사지에 적힌 원문. 미매칭이면 이것만 보여준다 */
        String ocrLabel,
        String category,
        ResultType resultType,
        BigDecimal numberValue,
        String textValue,
        String rawValue,
        String unit,
        BigDecimal normalMin,
        BigDecimal normalMax,
        NormalRangeSource normalRangeSource,
        ResultStatus resultStatus,
        /** 안심 / 주의 / 위험. UNKNOWN이면 null */
        String statusLabel,
        String description,
        /** 이번 수치가 왜 그 판정인지. 코드가 만든 문장이라 판정과 어긋나지 않는다 */
        String verdict,
        boolean isEditedByUser,
        Highlight highlight
) {
    /** 원본 이미지 위 하이라이트. 0~1 정규화 비율이며 픽셀 절대값이 아니다. */
    public record Highlight(Integer page, BigDecimal x, BigDecimal y,
                            BigDecimal width, BigDecimal height) {}

    public static TestResultResponse from(TestResult r) {
        var item = r.getTestItem();

        Highlight highlight = r.getBboxPage() == null ? null
                : new Highlight(r.getBboxPage(), r.getBboxX(), r.getBboxY(),
                r.getBboxWidth(), r.getBboxHeight());

        return new TestResultResponse(
                r.getId(),
                item == null ? null : item.getId(),
                item == null ? null : item.getNameKo(),
                item == null ? null : item.getNameEn(),
                r.getOcrLabel(),
                item == null ? r.getOcrCategory() : item.getCategory(),
                r.getResultType(),
                r.getNumberValue(),
                r.getTextValue(),
                r.getRawValue(),
                r.getUnit(),
                resolveMin(r),
                resolveMax(r),
                r.getNormalRangeSource(),
                r.getResultStatus(),
                label(r.getResultStatus()),
                item == null ? null : item.getBriefForMom(),
                r.getBriefForMom(),
                r.isEditedByUser(),
                highlight);
    }

    /** 화면에는 실제로 판정에 쓴 범위를 보여준다 */
    private static BigDecimal resolveMin(TestResult r) {
        if (r.getNormalRangeSource() == NormalRangeSource.SHEET) return r.getSheetNormalMin();
        return r.getTestItem() == null ? null : r.getTestItem().getNormalMin();
    }

    private static BigDecimal resolveMax(TestResult r) {
        if (r.getNormalRangeSource() == NormalRangeSource.SHEET) return r.getSheetNormalMax();
        return r.getTestItem() == null ? null : r.getTestItem().getNormalMax();
    }

    private static String label(ResultStatus status) {
        return switch (status) {
            case NORMAL -> "안심";
            case CAUTION -> "주의";
            case DANGER -> "위험";
            case UNKNOWN -> null;
        };
    }
}

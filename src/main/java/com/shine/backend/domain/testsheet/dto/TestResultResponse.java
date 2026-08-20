package com.shine.backend.domain.testsheet.dto;

import com.shine.backend.domain.compat.dto.ParsedTestItemDto;
import com.shine.backend.domain.compat.service.EngineMetaCodec;
import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestResult;

import java.math.BigDecimal;
import java.util.List;

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
        /** 안심 / 주의 / 위험 / 확인 필요 — 프론트가 아는 네 가지뿐이다 */
        String statusLabel,
        String description,
        /** 이번 수치가 왜 그 판정인지. 코드가 만든 문장이라 판정과 어긋나지 않는다 */
        String verdict,
        boolean isEditedByUser,
        Highlight highlight,

        // ---------- 업로드 때 저장해둔 프론트 판정 엔진의 근거 ----------
        // 이게 없으면 기록 탭에서 지난 검사지를 열었을 때 근거·출처·추천 질문이
        // 통째로 사라진다(전달사항 2번). 저장한 것을 그대로 돌려준다.

        String engineStatus,
        String badgeLabel,
        String basisLabel,
        String contrastNote,
        List<String> caveats,
        List<ParsedTestItemDto.CitationDto> citations,
        String doctorQuestion,
        String trendNote,
        Boolean needsConfirm,
        /** 검사지에 인쇄돼 있던 원문 항목명 */
        String originalName
) {
    /** 원본 이미지 위 하이라이트. 0~1 정규화 비율이며 픽셀 절대값이 아니다. */
    public record Highlight(Integer page, BigDecimal x, BigDecimal y,
                            BigDecimal width, BigDecimal height) {}

    public static TestResultResponse from(TestResult r) {
        return from(r, null);
    }

    public static TestResultResponse from(TestResult r, EngineMetaCodec.EngineMeta meta) {
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
                r.getResultStatus().label(),
                item == null ? null : item.getBriefForMom(),
                r.getBriefForMom(),
                r.isEditedByUser(),
                highlight,
                meta == null ? r.getEngineStatus() : meta.engineStatus(),
                meta == null ? null : meta.badgeLabel(),
                meta == null ? null : meta.basisLabel(),
                meta == null ? null : meta.contrastNote(),
                meta == null ? null : meta.caveats(),
                meta == null ? null : meta.citations(),
                meta == null ? null : meta.doctorQuestion(),
                meta == null ? null : meta.trendNote(),
                meta == null ? null : meta.needsConfirm(),
                meta == null ? null : meta.originalName());
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
}

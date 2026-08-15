package com.shine.backend.domain.testsheet.evaluator;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.NormalRangeSource;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.parser.ParsedValue;
import com.shine.backend.domain.testsheet.parser.QualitativeNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 검사 항목의 정상/주의/위험을 판정한다.
 *
 * AI가 아니라 코드가 한다. 결정론적이고 테스트 가능해야 하기 때문이다.
 * AI는 이미 나온 판정을 문장으로 설명만 한다.
 *
 * 애매하면 항상 이상 쪽으로 기운다 —
 * 이상 수치를 정상이라 말하는 것이 이 앱에서 가장 위험한 실패다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultEvaluator {

    private static final String VERDICT_PENDING = "미결";
    private static final String VERDICT_NORMAL = "정상";

    private final QualitativeNormalizer qualitativeNormalizer;

    public EvaluationResult evaluate(EvaluationInput input) {
        EvaluationResult result = judge(input);
        return crossCheckWithSheetVerdict(result, input);
    }

    private EvaluationResult judge(EvaluationInput input) {
        ParsedValue value = input.value();
        TestItemCatalog item = input.item();

        // ① 미매칭 항목 — 원문만 보여주고 AI도 호출하지 않는다
        if (item == null || value == null) {
            return EvaluationResult.unknown();
        }

        // ② 검사지가 판단을 보류한 경우
        if (VERDICT_PENDING.equals(trim(input.sheetVerdict()))) {
            return EvaluationResult.unknown();
        }

        // ③ 정성 판정. MIXED도 텍스트 부분으로 판정한다 ("음성(0.07)" → 음성)
        if (item.getResultType() == ResultType.TEXT || item.getResultType() == ResultType.MIXED) {
            return judgeQualitative(value, item);
        }

        return judgeQuantitative(value, item, input);
    }

    private EvaluationResult judgeQualitative(ParsedValue value, TestItemCatalog item) {
        String normalized = qualitativeNormalizer.normalize(value.textValue());

        // 사전에 없는 표기 — 추측하지 않는다
        if (normalized == null || item.getNormalText() == null) {
            return EvaluationResult.unknown();
        }

        // 검사지에 인쇄된 열거형 참고치("양성,음성,약양성")는 쓰지 않는다.
        // 그건 정상값 목록이 아니라 나올 수 있는 값 목록이라, 그대로 믿으면
        // 양성인 결과를 정상으로 판정하게 된다.
        ResultStatus status = normalized.equals(item.getNormalText())
                ? ResultStatus.NORMAL
                : ResultStatus.DANGER;

        return new EvaluationResult(status, NormalRangeSource.CATALOG, false);
    }

    private EvaluationResult judgeQuantitative(ParsedValue value, TestItemCatalog item,
                                               EvaluationInput input) {
        if (value.numberValue() == null) {
            return EvaluationResult.unknown();
        }

        BigDecimal min;
        BigDecimal max;
        NormalRangeSource source;

        if (item.isPregnancySpecific()) {
            // ★ 임신 중 정상범위가 크게 다른 항목.
            // 검사지 참고치는 비임신 성인 기준으로 찍혀 나오는 경우가 많아,
            // 그대로 쓰면 정상인 산모를 이상으로 표시하게 된다.
            min = item.getNormalMin();
            max = item.getNormalMax();
            source = NormalRangeSource.CATALOG;
        } else if (input.sheetNormalMin() != null || input.sheetNormalMax() != null) {
            // 일반 항목은 검사지에 인쇄된 참고치를 우선한다.
            // 병원·검사실별 기준 차이가 자동으로 해결된다.
            min = input.sheetNormalMin();
            max = input.sheetNormalMax();
            source = NormalRangeSource.SHEET;
        } else {
            min = item.getNormalMin();
            max = item.getNormalMax();
            source = NormalRangeSource.CATALOG;
        }

        if (min == null && max == null) {
            return EvaluationResult.unknown();
        }

        BigDecimal v = value.numberValue();

        // ④ 물리적으로 불가능한 값 — OCR 자릿수 오독을 의심한다 (12.0을 120으로 읽는 경우)
        if (isBeyondHardLimit(v, item)) {
            log.warn("하드리밋 초과 item={} value={}", item.getCode(), v);
            return EvaluationResult.unknown();
        }

        boolean aboveMin = min == null || v.compareTo(min) >= 0;
        boolean belowMax = max == null || v.compareTo(max) <= 0;

        if (aboveMin && belowMax) {
            return new EvaluationResult(ResultStatus.NORMAL, source, false);
        }

        // ⑤ 경계값은 주의로 둔다. 폭은 항목별로 조정 가능하다.
        if (min != null && max != null) {
            BigDecimal margin = max.subtract(min).multiply(item.getCautionMarginRatio());
            boolean withinMargin = v.compareTo(min.subtract(margin)) >= 0
                    && v.compareTo(max.add(margin)) <= 0;
            if (withinMargin) {
                return new EvaluationResult(ResultStatus.CAUTION, source, false);
            }
        }

        return new EvaluationResult(ResultStatus.DANGER, source, false);
    }

    private boolean isBeyondHardLimit(BigDecimal v, TestItemCatalog item) {
        if (item.getHardLimitMin() != null && v.compareTo(item.getHardLimitMin()) < 0) return true;
        return item.getHardLimitMax() != null && v.compareTo(item.getHardLimitMax()) > 0;
    }

    /**
     * 검사지에 이미 "정상"이라고 인쇄돼 있는데 우리는 이상이라고 판단한 경우.
     *
     * 우리 참고치가 틀렸을 수도, 검사지가 비임신 기준일 수도 있다.
     * 어느 쪽이 맞는지 모를 때는 NORMAL로 내리지 않고 CAUTION에 둔다.
     * 불일치를 기록해두면 관리자가 모아 보고 데이터 문제를 찾을 수 있다.
     */
    private EvaluationResult crossCheckWithSheetVerdict(EvaluationResult result, EvaluationInput input) {
        if (!VERDICT_NORMAL.equals(trim(input.sheetVerdict()))) {
            return result;
        }
        if (result.status() == ResultStatus.NORMAL || result.status() == ResultStatus.UNKNOWN) {
            return result;
        }

        log.warn("판정 불일치 item={} value={} sheet=정상 ours={}",
                input.item() == null ? "미매칭" : input.item().getCode(),
                input.value() == null ? null : input.value().rawValue(),
                result.status());

        return new EvaluationResult(ResultStatus.CAUTION, result.source(), true);
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}

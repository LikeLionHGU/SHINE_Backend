package com.shine.backend.domain.testsheet.analyzer;

import com.shine.backend.domain.testitem.entity.ResultType;
import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import org.springframework.stereotype.Component;

/**
 * "이번 수치가 왜 그 판정인지"를 한 문장으로 만든다.
 *
 * AI가 아니라 코드가 만든다. 판정과 숫자가 어긋나면 안 되기 때문이다.
 * 진단명을 쓰지 않고, 이상일 때는 항상 의사 확인을 권하며 끝낸다.
 */
@Component
public class VerdictGenerator {

    /** @return 판정을 못 한 항목이면 null. 호출부가 원문을 그대로 쓰면 된다 */
    public String generate(AnalyzedRow row) {
        TestItemCatalog item = row.item();
        if (item == null || row.resultStatus() == ResultStatus.UNKNOWN) return null;

        String name = item.getNameKo();

        // 정량 — 범위라는 개념이 있다
        if (row.resultType() == ResultType.NUMBER && row.numberValue() != null) {
            String tail = switch (row.resultStatus()) {
                case NORMAL -> "정상 범위 안에 있어요.";
                case CAUTION -> "정상 범위 경계에 있어요. 선생님과 이야기해 보세요.";
                case DANGER -> "정상 범위를 벗어났어요. 선생님과 이야기해 보세요.";
                default -> "";
            };
            String unit = row.unit() == null ? "" : " " + row.unit();
            return "%s 수치가 %s%s로 %s".formatted(
                    name, row.numberValue().stripTrailingZeros().toPlainString(), unit, tail);
        }

        // 정성 — 음성/양성에는 범위가 없다. "정상 범위"라는 표현은 어색하다
        if (row.textValue() != null) {
            String tail = switch (row.resultStatus()) {
                case NORMAL -> "이번 검사에서는 이상이 확인되지 않았어요.";
                case CAUTION -> "한 번 더 확인이 필요할 수 있어요. 선생님과 이야기해 보세요.";
                case DANGER -> "선생님과 꼭 이야기해 보세요.";
                default -> "";
            };
            return "%s 결과가 %s으로 나왔어요.\n%s".formatted(name, row.textValue(), tail);
        }
        return null;
    }
}

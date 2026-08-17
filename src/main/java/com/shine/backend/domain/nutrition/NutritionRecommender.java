package com.shine.backend.domain.nutrition;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 검사 결과에서 부족한 영양소를 찾아 재료를 추천한다.
 *
 * ⚠️ v1 초안이다. 규칙이 늘어나면 JSON 리소스로 옮긴다.
 * 숫자(섭취 권장량)는 절대 넣지 않는다 — 용량을 명시하면 약물·용량 권고에 해당한다.
 *
 * 임신 중 금기 식품은 목록에서 의도적으로 제외했다.
 *   동물의 간   레티놀 과다 → 선천성 기형 위험
 *   참치 대뱃살·상어·황새치   메틸수은
 *   다시마   요오드 과다
 */
@Component
public class NutritionRecommender {

    public record Food(String name, String nutrient, String reason, String relatedItemName) {}

    private record Rule(String nutrient, List<String> foods, String reason) {}

    /** 검사 항목 코드 → 그 항목이 낮을 때 도움되는 영양소 */
    private static final Map<String, Rule> RULES = Map.of(
            "HB", new Rule("철분",
                    List.of("시금치", "소고기", "검은콩", "굴"),
                    "혈색소가 낮을 때 도움이 되는 재료예요."),
            "FERRITIN", new Rule("철분",
                    List.of("시금치", "소고기", "렌틸콩", "굴"),
                    "저장 철분이 부족할 때 참고하면 좋아요."),
            "HCT", new Rule("철분",
                    List.of("시금치", "소고기", "검은콩", "참깨"),
                    "적혈구 수치가 낮을 때 도움이 되는 재료예요."),
            "VIT_D", new Rule("비타민 D",
                    List.of("연어", "표고버섯", "달걀", "고등어"),
                    "비타민 D가 부족할 때 참고하면 좋아요."),
            "FOLATE", new Rule("엽산",
                    List.of("시금치", "브로콜리", "아보카도", "렌틸콩"),
                    "엽산이 풍부한 재료예요."),
            "VIT_B12", new Rule("비타민 B12",
                    List.of("달걀", "우유", "소고기", "굴"),
                    "비타민 B12가 부족할 때 참고하면 좋아요."),
            "TP", new Rule("단백질",
                    List.of("달걀", "두부", "닭가슴살", "그릭요거트"),
                    "단백질이 부족할 때 도움이 되는 재료예요."),
            "ALB", new Rule("단백질",
                    List.of("달걀", "두부", "닭가슴살", "연어"),
                    "단백질이 부족할 때 도움이 되는 재료예요.")
    );

    /** 비타민 C는 철분 흡수를 돕는다. 철분을 추천할 때 하나 끼워 넣는다. */
    private static final Food VITAMIN_C =
            new Food("파프리카", "비타민 C", "철분이 잘 흡수되도록 도와줘요.", null);

    /**
     * @param lowItemCodes 정상 범위보다 낮게 나온 항목 코드들
     * @return 중복을 제거한 추천 재료. 부족한 항목이 없으면 빈 목록
     */
    public List<Food> recommend(Collection<String> lowItemCodes, Map<String, String> itemNames, int size) {
        LinkedHashMap<String, Food> picked = new LinkedHashMap<>();
        boolean hasIron = false;

        for (String code : lowItemCodes) {
            Rule rule = RULES.get(code);
            if (rule == null) continue;
            if ("철분".equals(rule.nutrient())) hasIron = true;

            for (String food : rule.foods()) {
                picked.putIfAbsent(food,
                        new Food(food, rule.nutrient(), rule.reason(), itemNames.get(code)));
            }
        }

        if (hasIron) picked.putIfAbsent(VITAMIN_C.name(), VITAMIN_C);

        return picked.values().stream().limit(size).toList();
    }
}

package com.shine.backend.domain.nutrition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 추천 가능한 음식 목록. 이 서비스의 단일 출처다.
 *
 * 목록을 고정하는 이유가 두 가지다.
 *   ① 디자이너가 이미지를 준비할 수 있다
 *   ② 임신 중 금기 식품이 섞이는 것을 막는다
 *      동물의 간(레티놀 과다 → 선천성 기형), 참치 대뱃살·상어(메틸수은),
 *      다시마(요오드 과다) 는 의도적으로 뺐다.
 *
 * 프론트 프롬프트에 목록을 넣어도 AI가 가끔 벗어나므로 서버에서 한 번 더 거른다.
 */
@Slf4j
@Component
public class AllowedFoods {

    /** 화면에 보여줄 순서대로 */
    private static final List<String> NAMES = List.of(
            // 채소·버섯
            "시금치", "브로콜리", "파프리카", "표고버섯", "케일", "당근", "토마토", "아스파라거스",
            // 과일
            "아보카도", "딸기", "오렌지", "키위", "바나나",
            // 콩·두부
            "두부", "검은콩", "렌틸콩",
            // 곡류·서류
            "고구마", "귀리", "현미",
            // 어패류
            "연어", "고등어", "굴",
            // 육류
            "소고기", "닭가슴살",
            // 알·유제품
            "달걀", "우유", "그릭요거트",
            // 해조류·견과
            "김", "미역", "참깨");

    /** AI가 흔히 쓰는 다른 표현들 */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("계란", "달걀"),
            Map.entry("달걀노른자", "달걀"),
            Map.entry("소고기살코기", "소고기"),
            Map.entry("쇠고기", "소고기"),
            Map.entry("닭고기", "닭가슴살"),
            Map.entry("닭가슴살살", "닭가슴살"),
            Map.entry("요거트", "그릭요거트"),
            Map.entry("그릭요구르트", "그릭요거트"),
            Map.entry("요구르트", "그릭요거트"),
            Map.entry("검정콩", "검은콩"),
            Map.entry("서리태", "검은콩"),
            Map.entry("렌즈콩", "렌틸콩"),
            Map.entry("표고", "표고버섯"),
            Map.entry("버섯", "표고버섯"),
            Map.entry("김치", "김"),
            Map.entry("구운김", "김"),
            Map.entry("고구마순", "고구마"),
            Map.entry("참깨가루", "참깨"),
            Map.entry("깨", "참깨"),
            Map.entry("우유(저지방)", "우유"),
            Map.entry("저지방우유", "우유"));

    private static final Map<String, String> INDEX = new HashMap<>();

    static {
        NAMES.forEach(n -> INDEX.put(key(n), n));
        ALIASES.forEach((alias, canonical) -> INDEX.put(key(alias), canonical));
    }

    private static String key(String s) {
        return s == null ? "" : s.replaceAll("[\\s·,]", "");
    }

    /** @return 목록에 있으면 표준 이름, 없으면 null */
    public String canonicalize(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;
        String found = INDEX.get(key(rawName.trim()));
        if (found == null) {
            log.debug("목록에 없는 음식 제외: {}", rawName);
        }
        return found;
    }

    public boolean contains(String rawName) {
        return canonicalize(rawName) != null;
    }

    public List<String> all() {
        return NAMES;
    }
}

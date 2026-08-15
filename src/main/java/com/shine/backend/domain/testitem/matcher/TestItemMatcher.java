package com.shine.backend.domain.testitem.matcher;

import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import com.shine.backend.domain.testitem.repository.TestItemCatalogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OCR이 읽은 검사명을 카탈로그 항목과 연결한다.
 *
 * 설계결정④: 병원별 표기 차이를 코드의 if문이 아니라 데이터(name_variants)로 흡수한다.
 * 새 표기가 나오면 DB에 한 줄 추가하면 되고, 코드는 건드리지 않는다.
 *
 * 서버 기동 시 전체를 메모리에 올린다. 검사지 한 장에 항목이 20~30개인데
 * 매번 DB를 조회하면 그만큼 쿼리가 나간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestItemMatcher {

    private final TestItemCatalogRepository repository;

    /** 정규화된 표기 → 항목 */
    private volatile Map<String, TestItemCatalog> index = Map.of();

    @PostConstruct
    @Transactional(readOnly = true)
    public void refresh() {
        List<TestItemCatalog> items = repository.findAll();
        Map<String, TestItemCatalog> built = new HashMap<>();

        for (TestItemCatalog item : items) {
            register(built, item.getCode(), item);
            register(built, item.getNameKo(), item);
            register(built, item.getNameEn(), item);
            if (item.getNameVariants() != null) {
                item.getNameVariants().forEach(v -> register(built, v, item));
            }
        }

        this.index = built;
        log.info("검사 항목 사전 로드 완료 — 항목 {}개, 표기 {}개", items.size(), built.size());
    }

    private void register(Map<String, TestItemCatalog> map, String label, TestItemCatalog item) {
        String key = LabelNormalizer.key(label);
        if (key == null || key.length() < 2) return;

        TestItemCatalog existing = map.putIfAbsent(key, item);
        // 서로 다른 항목이 같은 표기를 쓰면 잘못 붙을 수 있다. 시딩 데이터를 고쳐야 한다.
        if (existing != null && !existing.getCode().equals(item.getCode())) {
            log.warn("표기 충돌 '{}' — {} vs {}", label, existing.getCode(), item.getCode());
        }
    }

    /**
     * @return 매칭된 항목. 실패하면 empty — 이때 호출부는 ocr_label에 원문을 남기고
     *         result_status를 UNKNOWN으로 둔다. 데이터를 버리지 않는다(설계결정⑤).
     */
    public Optional<TestItemCatalog> match(String ocrLabel) {
        for (String candidate : LabelNormalizer.candidates(ocrLabel)) {
            TestItemCatalog found = index.get(candidate);
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    public int size() {
        return index.size();
    }
}

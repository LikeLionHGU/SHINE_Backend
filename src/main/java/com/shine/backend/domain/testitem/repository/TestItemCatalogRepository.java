package com.shine.backend.domain.testitem.repository;

import com.shine.backend.domain.testitem.entity.TestItemCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 카탈로그는 서버 기동 시 전부 메모리에 캐싱하는 전제다(설계결정④).
 * 검사지를 파싱할 때마다 DB를 조회하면 항목 수만큼 쿼리가 나간다.
 */
public interface TestItemCatalogRepository extends JpaRepository<TestItemCatalog, Long> {

    Optional<TestItemCatalog> findByCode(String code);
}

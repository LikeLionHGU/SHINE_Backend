package com.shine.backend.domain.testsheet.repository;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestSheetRepository extends JpaRepository<TestSheet, Long> {

    /** 홈 화면 — 가장 최근 분석 완료된 검사지 1건 */
    Optional<TestSheet> findFirstByUserIdAndAnalysisStatusOrderByTestDateDesc(
            Long userId, AnalysisStatus analysisStatus);

    /** 기록 타임라인 — 커서 페이지네이션. cursor보다 id가 작은 것들을 최신순으로 */
    List<TestSheet> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor);

    List<TestSheet> findByUserIdOrderByIdDesc(Long userId);
}

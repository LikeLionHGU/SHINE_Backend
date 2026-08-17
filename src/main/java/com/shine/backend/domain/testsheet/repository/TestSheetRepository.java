package com.shine.backend.domain.testsheet.repository;

import com.shine.backend.domain.testsheet.entity.AnalysisStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TestSheetRepository extends JpaRepository<TestSheet, Long> {

    /** 홈 화면 — 가장 최근 분석 완료된 검사지 1건 */
    Optional<TestSheet> findFirstByUserIdAndAnalysisStatusOrderByTestDateDesc(
            Long userId, AnalysisStatus analysisStatus);

    /** 기록 타임라인 — 커서 페이지네이션. cursor보다 id가 작은 것들을 최신순으로 */
    List<TestSheet> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    List<TestSheet> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 분석 탭 — 특정 검사일의 검사지.
     * 상태 조건을 쿼리에 넣는다. 한 장만 가져온 뒤 코드에서 거르면
     * 같은 날짜에 여러 장이 있을 때 엉뚱한 것이 잡혀 결과가 비어버린다.
     */
    Optional<TestSheet> findFirstByUserIdAndAnalysisStatusAndTestDateOrderByIdDesc(
            Long userId, AnalysisStatus analysisStatus, LocalDate testDate);

    /** 분석 탭 날짜 이동 — 이전/다음 검사일 */
    Optional<TestSheet> findFirstByUserIdAndAnalysisStatusAndTestDateLessThanOrderByTestDateDescIdDesc(
            Long userId, AnalysisStatus analysisStatus, LocalDate testDate);

    Optional<TestSheet> findFirstByUserIdAndAnalysisStatusAndTestDateGreaterThanOrderByTestDateAscIdAsc(
            Long userId, AnalysisStatus analysisStatus, LocalDate testDate);
}

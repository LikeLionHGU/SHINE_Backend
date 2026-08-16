package com.shine.backend.domain.testsheet.repository;

import com.shine.backend.domain.testsheet.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    /**
     * ★ 추이 그래프 — 앱에서 가장 자주 실행되는 쿼리.
     * idx_results_trend(user_id, test_item_id, test_date)로 커버되며 조인이 발생하지 않는다.
     * r.user.id / r.testItem.id 는 FK 컬럼을 직접 읽으므로 조인이 아니다.
     */
    @Query("""
            SELECT r FROM TestResult r
            WHERE r.user.id = :userId AND r.testItem.id = :testItemId
            ORDER BY r.testDate ASC
            """)
    List<TestResult> findTrend(Long userId, Long testItemId);

    /** 검사지 상세(번역 화면) — 카탈로그를 함께 가져와 N+1을 막는다 */
    @Query("""
            SELECT r FROM TestResult r
            LEFT JOIN FETCH r.testItem
            WHERE r.testSheet.id = :testSheetId
            """)
    List<TestResult> findBySheetWithItem(Long testSheetId);

    /** 분석 탭 — 특정 검사지의 항목 목록 */
    List<TestResult> findByTestSheetId(Long testSheetId);

    /**
     * 운영용 — 매칭 실패한 라벨을 빈도순으로 집계한다(설계결정⑤).
     * 자주 나오는 라벨부터 name_variants에 추가하면 커버리지가 올라간다.
     */
    @Query("""
            SELECT r.ocrLabel, COUNT(r) FROM TestResult r
            WHERE r.testItem IS NULL
            GROUP BY r.ocrLabel
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countUnmatchedLabels();

    /**
     * 여러 검사지의 판정별 개수를 한 번에 센다.
     * 타임라인에서 검사지마다 따로 세면 N+1이 된다.
     * 반환: [testSheetId, ResultStatus, count]
     */
    @Query("""
            SELECT r.testSheet.id, r.resultStatus, COUNT(r)
            FROM TestResult r
            WHERE r.testSheet.id IN :sheetIds
            GROUP BY r.testSheet.id, r.resultStatus
            """)
    List<Object[]> countStatusBySheetIds(List<Long> sheetIds);

    /** 분석 탭 스파크라인 — 여러 항목의 추이를 한 번에 가져온다 */
    @Query("""
            SELECT r FROM TestResult r
            WHERE r.user.id = :userId AND r.testItem.id IN :itemIds
            ORDER BY r.testDate ASC
            """)
    List<TestResult> findTrendsByItemIds(Long userId, List<Long> itemIds);
}

-- =====================================================================
--  마이그레이션 — 프론트 판정 엔진 도입 대응 (2026-08-21)
--  기준 문서: 인수인계_2026-08-19.md
--
--  MySQL 8.0 에는 ADD COLUMN IF NOT EXISTS 가 없다(그건 MariaDB 문법이다).
--  이미 있는 컬럼에 두 번 돌려도 실패하지 않도록 information_schema 를 보고
--  필요할 때만 ALTER 를 실행한다. 여러 번 돌려도 안전하다.
--
--  DROP 없음. 운영 DB에 그대로 실행할 수 있다.
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 판정 근거 보관 (전달사항 2번)
--
--    저장하지 않으면 기록 탭에서 지난 검사지를 열었을 때 근거·출처·추천 질문이
--    통째로 사라진다. 서버는 이 값을 읽거나 조건에 걸지 않고 그대로 돌려주기만
--    하므로 컬럼을 쪼개지 않고 JSON 한 칸에 담는다.
-- ---------------------------------------------------------------------
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'test_results'
        AND COLUMN_NAME = 'engine_status') > 0,
    'SELECT ''test_results.engine_status 이미 있음'' AS skipped',
    'ALTER TABLE test_results ADD COLUMN engine_status VARCHAR(20) NULL
       COMMENT ''safe|watch|recheck|indeterminate|alert|info_only|unsupported'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'test_results'
        AND COLUMN_NAME = 'engine_meta') > 0,
    'SELECT ''test_results.engine_meta 이미 있음'' AS skipped',
    'ALTER TABLE test_results ADD COLUMN engine_meta JSON NULL
       COMMENT ''판정 근거·출처·인용문·추천 질문 원본(프론트 엔진 산출물)'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 스키마 드리프트 정리
--
--    01_schema.sql 이 엔티티보다 뒤처져 있다. 아래 두 컬럼은 코드가 이미 쓰고
--    있는데 DDL 에는 없어서, 깨끗한 DB 에 스키마를 새로 깔면 기동이 막힌다.
--    이미 손으로 ALTER 해둔 서버라면 그냥 건너뛴다.
-- ---------------------------------------------------------------------
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointments'
        AND COLUMN_NAME = 'client_id') > 0,
    'SELECT ''appointments.client_id 이미 있음'' AS skipped',
    'ALTER TABLE appointments ADD COLUMN client_id VARCHAR(64) NULL
       COMMENT ''프론트가 만든 일정 id(visit-2026-08-16)'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'test_sheets'
        AND COLUMN_NAME = 'nutrition_foods') > 0,
    'SELECT ''test_sheets.nutrition_foods 이미 있음'' AS skipped',
    'ALTER TABLE test_sheets ADD COLUMN nutrition_foods JSON NULL
       COMMENT ''프론트 AI가 추천한 재료'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. 상태값 (전달사항 1번) — 할 일 없음
--
--    result_status 는 ENUM('NORMAL','CAUTION','DANGER','UNKNOWN') 이라
--    한글 "미분류"가 쌓인 적이 없다. 화면에 나가는 문자열만
--    ResultStatus.label() 에서 "확인 필요"로 바뀐다.
-- ---------------------------------------------------------------------

-- 결과 확인
SELECT COLUMN_NAME, COLUMN_TYPE
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'test_results'
   AND COLUMN_NAME LIKE 'engine%';

-- ---------------------------------------------------------------------
-- 되돌리기
--   ALTER TABLE test_results DROP COLUMN engine_status, DROP COLUMN engine_meta;
--   (2번의 두 컬럼은 코드가 쓰고 있으므로 되돌리면 안 된다)
-- ---------------------------------------------------------------------

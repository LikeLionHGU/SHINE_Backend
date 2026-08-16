-- =====================================================================
--  산전 검사지 AI 앱 — DDL v1.0  (MySQL 8.0 / InnoDB / utf8mb4)
--  주의: DROP 포함. 운영 DB에서 실행하지 말 것.
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS test_results;
DROP TABLE IF EXISTS test_sheets;
DROP TABLE IF EXISTS test_item_catalog;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. users --------------------------------------------------------------
-- 설계결정①: 주수를 저장하지 않고 last_period_date만 저장해 매번 계산한다.
CREATE TABLE users (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    login_id                VARCHAR(20)     NOT NULL COMMENT '4~20자 영문소문자+숫자',
    password                VARCHAR(60)     NOT NULL COMMENT 'BCrypt 해시',
    name                    VARCHAR(20)     NOT NULL,
    nickname                VARCHAR(30)     NULL,
    profile_image_key       VARCHAR(255)    NULL COMMENT 'S3 key',
    phone_number            VARCHAR(20)     NOT NULL,
    email                   VARCHAR(100)    NOT NULL COMMENT '본인 이메일',
    guardian_email          VARCHAR(100)    NULL COMMENT '보호자',
    additional_email        VARCHAR(100)    NULL,
    last_period_date        DATE            NOT NULL COMMENT '주수의 유일한 원천',
    camera_agreed           BOOLEAN         NOT NULL DEFAULT FALSE,
    notification_enabled    BOOLEAN         NOT NULL DEFAULT TRUE,
    terms_agreed_at         DATETIME        NOT NULL,
    privacy_agreed_at       DATETIME        NOT NULL,
    sensitive_agreed_at     DATETIME        NOT NULL COMMENT '민감정보 별도 동의',
    login_fail_count        INT             NOT NULL DEFAULT 0,
    locked_until            DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_login_id (login_id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='사용자';

-- 2. test_item_catalog --------------------------------------------------
-- 검사지의 자식이 아니라 앱 전체가 공유하는 사전. 사용자가 늘어도 행 수는 그대로.
CREATE TABLE test_item_catalog (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(30)     NOT NULL COMMENT 'HB, WBC, ALT ...',
    name_ko                 VARCHAR(50)     NOT NULL,
    name_en                 VARCHAR(100)    NULL,
    category                VARCHAR(30)     NOT NULL COMMENT 'HEMATOLOGY / CHEMISTRY / IMMUNO_SEROLOGY / URINALYSIS / IMAGING / GROUP',
    result_type             ENUM('NUMBER','TEXT','MIXED') NOT NULL DEFAULT 'NUMBER' COMMENT 'MIXED = 음성(0.07) 형태',
    unit                    VARCHAR(30)     NULL,
    normal_min              DECIMAL(12,4)   NULL,
    normal_max              DECIMAL(12,4)   NULL,
    normal_text             VARCHAR(50)     NULL COMMENT '정성 항목 정상값',
    hard_limit_min          DECIMAL(12,4)   NULL COMMENT 'OCR 자릿수 오독 탐지',
    hard_limit_max          DECIMAL(12,4)   NULL,
    caution_margin_ratio    DECIMAL(4,3)    NOT NULL DEFAULT 0.100,
    is_pregnancy_specific   BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'TRUE면 검사지 참고치 무시하고 카탈로그 임신기준 사용',
    is_trendable            BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'FALSE면 추이 그래프 제외',
    name_variants           JSON            NOT NULL COMMENT '["혈색소","Hb","HGB"] 병원별 표기 흡수',
    brief_for_mom           VARCHAR(500)    NULL,
    brief_for_doctor        VARCHAR(500)    NULL,
    display_order           INT             NOT NULL DEFAULT 999,
    loinc_code              VARCHAR(20)     NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_catalog_code (code),
    KEY idx_catalog_category (category, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='검사 항목 사전(마스터)';

-- 3. test_sheets --------------------------------------------------------
-- 설계결정③: OCR 원본을 통째로 보존해 파서 개선 후 재파싱할 수 있게 한다.
CREATE TABLE test_sheets (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    test_date               DATE            NOT NULL,
    test_date_confirmed     BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'OCR이 날짜를 못 읽으면 FALSE',
    pregnancy_week          INT             NOT NULL COMMENT '검사 시점 주수 스냅샷',
    hospital_name           VARCHAR(100)    NULL,
    sheet_issued_date       DATE            NULL,
    image_keys              JSON            NOT NULL COMMENT 'S3 key 배열. 순서=페이지',
    analysis_status         ENUM('WAITING','ANALYZING','DONE','FAILED') NOT NULL DEFAULT 'WAITING',
    failure_reason          VARCHAR(30)     NULL,
    ocr_raw_json            JSON            NULL COMMENT '재파싱의 유일한 근거',
    ocr_engine              VARCHAR(30)     NULL,
    pii_masked              BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '이름·주소 마스킹 완료 여부',
    summary_for_mom         VARCHAR(2000)   NULL,
    summary_for_doctor      VARCHAR(2000)   NULL,
    llm_model               VARCHAR(50)     NULL,
    prompt_version          VARCHAR(20)     NULL,
    analyzed_at             DATETIME        NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sheets_user_date (user_id, test_date DESC),
    KEY idx_sheets_user_status (user_id, analysis_status, test_date DESC),
    CONSTRAINT fk_sheets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='검사지 1건';

-- 4. test_results -------------------------------------------------------
-- 설계결정②: 행 단위 분리는 타협 불가. JSON으로 합치면 추이 그래프가 불가능해진다.
-- 설계결정⑧: user_id/test_date/pregnancy_week는 의도적 비정규화(조인 없는 추이 조회).
CREATE TABLE test_results (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    test_sheet_id           BIGINT          NOT NULL,
    test_item_id            BIGINT          NULL COMMENT '설계결정⑤: NULL이면 미매칭. 버리지 않고 보존',
    user_id                 BIGINT          NOT NULL,
    test_date               DATE            NOT NULL,
    pregnancy_week          INT             NOT NULL,
    ocr_label               VARCHAR(100)    NOT NULL COMMENT '검사명 원문',
    ocr_category            VARCHAR(30)     NULL,
    raw_value               VARCHAR(100)    NOT NULL COMMENT '결과 원문. 음성(0.07), RH(+), 12.2',
    result_type             ENUM('NUMBER','TEXT','MIXED') NULL,
    number_value            DECIMAL(12,4)   NULL,
    text_value              VARCHAR(100)    NULL COMMENT '정규화된 표준값',
    unit                    VARCHAR(30)     NULL,
    unit_raw                VARCHAR(30)     NULL COMMENT 'OCR 원문 단위(g/dl, M/UL)',
    sheet_normal_min        DECIMAL(12,4)   NULL,
    sheet_normal_max        DECIMAL(12,4)   NULL,
    sheet_normal_text       VARCHAR(255)    NULL COMMENT '열거형은 보존만, 판정에 쓰지 않음',
    normal_range_source     ENUM('SHEET','CATALOG','NONE') NOT NULL DEFAULT 'NONE',
    result_status           ENUM('NORMAL','CAUTION','DANGER','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
    sheet_verdict           VARCHAR(20)     NULL COMMENT '검사지에 인쇄된 판정',
    verdict_mismatch        BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '우리 판정과 불일치. 품질 모니터링용',
    is_edited_by_user       BOOLEAN         NOT NULL DEFAULT FALSE,
    brief_for_mom           VARCHAR(500)    NULL,
    brief_for_doctor        VARCHAR(500)    NULL,
    bbox_page               INT             NULL,
    bbox_x                  DECIMAL(6,5)    NULL COMMENT '0~1 정규화 비율. 픽셀 절대값 아님',
    bbox_y                  DECIMAL(6,5)    NULL,
    bbox_width              DECIMAL(6,5)    NULL,
    bbox_height             DECIMAL(6,5)    NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_results_trend (user_id, test_item_id, test_date),
    KEY idx_results_sheet (test_sheet_id),
    KEY idx_results_unmatched (test_item_id, ocr_label),
    KEY idx_results_mismatch (verdict_mismatch, created_at),
    CONSTRAINT fk_results_sheet FOREIGN KEY (test_sheet_id) REFERENCES test_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_results_item  FOREIGN KEY (test_item_id)  REFERENCES test_item_catalog(id) ON DELETE SET NULL,
    CONSTRAINT fk_results_user  FOREIGN KEY (user_id)       REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='검사지에 적힌 값 1개';

-- 5. appointments -------------------------------------------------------
CREATE TABLE appointments (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    title                   VARCHAR(50)     NOT NULL,
    location                VARCHAR(100)    NULL,
    visit_at                DATETIME        NOT NULL,
    pregnancy_week          INT             NOT NULL COMMENT 'visit_at 기준 스냅샷',
    is_obgyn                BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '캘린더 마커 색 결정',
    visit_status            ENUM('SCHEDULED','DONE','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    created_by              ENUM('SYSTEM','USER') NOT NULL DEFAULT 'USER',
    schedule_code           VARCHAR(30)     NULL COMMENT 'prenatal_schedule.json 코드',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_appointments_user_visit (user_id, visit_at),
    KEY idx_appointments_regen (user_id, created_by, visit_at),
    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='병원 방문 일정';

-- 6. questions ----------------------------------------------------------
-- AI 챗봇이 아니라 진료 전에 적어두는 메모다. AI 답변을 저장하는 곳이 아님.
CREATE TABLE questions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    test_sheet_id           BIGINT          NULL,
    appointment_id          BIGINT          NULL,
    content                 VARCHAR(500)    NOT NULL,
    created_by              ENUM('AI','USER') NOT NULL DEFAULT 'USER',
    question_status         ENUM('PENDING','ANSWERED') NOT NULL DEFAULT 'PENDING',
    doctor_answer           VARCHAR(1000)   NULL,
    include_in_briefing     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_questions_user_status (user_id, question_status, created_at DESC),
    KEY idx_questions_sheet (test_sheet_id),
    KEY idx_questions_appointment (appointment_id),
    CONSTRAINT fk_questions_user        FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_questions_sheet       FOREIGN KEY (test_sheet_id)  REFERENCES test_sheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_questions_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='진료 때 물어볼 질문 메모';

-- =====================================================================
-- 레인아이(Rain-I) 1단계: DB 스키마
-- 실행: HeidiSQL "root" 세션 › 파일 › SQL 파일 열기 › 실행(F9)
-- 주의: 다시 실행하면 모든 테이블을 지우고 새로 만듭니다(데이터 삭제).
--       스키마를 바꾼 뒤에는 이 파일 → 02_seed_demo.sql 순서로 실행하세요.
-- 대상: MariaDB 12.3, DB raineye (utf8mb4)
-- =====================================================================

USE raineye;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS mileage_ledger;
DROP TABLE IF EXISTS verifications;
DROP TABLE IF EXISTS drain_status_log;
DROP TABLE IF EXISTS work_orders;
DROP TABLE IF EXISTS hotspots;
DROP TABLE IF EXISTS weather_forecast;
DROP TABLE IF EXISTS drains;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS districts;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 0) 행정동 참조표 (9개 핵심 테이블 외 보조 테이블)
--    district_code = 행정안전부 행정동 코드 10자리
--    center_lat/lng = 시연 지역 근처 동의 대략적인 중심점(근사값).
--    새 제보의 행정동은 "가장 가까운 중심점"으로 정합니다(3단계).
-- ---------------------------------------------------------------------
CREATE TABLE districts (
    district_code  CHAR(10)      NOT NULL,
    name           VARCHAR(20)   NOT NULL,
    center_lat     DECIMAL(10,7) NULL,
    center_lng     DECIMAL(10,7) NULL,
    PRIMARY KEY (district_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='성동구 행정동';

-- ---------------------------------------------------------------------
-- 1) users: 회원과 권한
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id          BIGINT        NOT NULL AUTO_INCREMENT,
    email            VARCHAR(100)  NOT NULL,
    password_hash    CHAR(60)      NOT NULL COMMENT 'jBCrypt $2a$ 해시',
    nickname         VARCHAR(30)   NOT NULL,
    role             ENUM('CITIZEN','OFFICIAL','CREW') NOT NULL DEFAULT 'CITIZEN',
    district_code    CHAR(10)      NULL,
    mileage_balance  INT           NOT NULL DEFAULT 0 COMMENT '원장 합계와 같은 트랜잭션에서 갱신',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_district (district_code),
    CONSTRAINT fk_users_district FOREIGN KEY (district_code) REFERENCES districts (district_code),
    CONSTRAINT ck_users_mileage CHECK (mileage_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

-- ---------------------------------------------------------------------
-- 2) drains: 빗물받이 마커와 현재 상태
-- ---------------------------------------------------------------------
CREATE TABLE drains (
    drain_id       BIGINT        NOT NULL AUTO_INCREMENT,
    lat            DECIMAL(10,7) NOT NULL,
    lng            DECIMAL(10,7) NOT NULL,
    address        VARCHAR(200)  NULL,
    district_code  CHAR(10)      NULL,
    status         ENUM('GOOD','CAUTION','DANGER') NOT NULL,
    reporter_id    BIGINT        NOT NULL COMMENT '최초 제보자',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (drain_id),
    KEY idx_drains_status (status),
    KEY idx_drains_district (district_code),
    KEY idx_drains_latlng (lat, lng),
    CONSTRAINT fk_drains_reporter FOREIGN KEY (reporter_id)   REFERENCES users (user_id),
    CONSTRAINT fk_drains_district FOREIGN KEY (district_code) REFERENCES districts (district_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='빗물받이';

-- ---------------------------------------------------------------------
-- 3) drain_status_log: 상태 변경 이력 + 증빙 사진 (INSERT만, 수정 금지)
--    최초 제보는 from_status = NULL
-- ---------------------------------------------------------------------
CREATE TABLE drain_status_log (
    log_id       BIGINT        NOT NULL AUTO_INCREMENT,
    drain_id     BIGINT        NOT NULL,
    user_id      BIGINT        NOT NULL COMMENT '변경한 사람',
    from_status  ENUM('GOOD','CAUTION','DANGER') NULL,
    to_status    ENUM('GOOD','CAUTION','DANGER') NOT NULL,
    photo_path   VARCHAR(255)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_log_drain_time (drain_id, created_at),
    KEY idx_log_user (user_id),
    CONSTRAINT fk_log_drain FOREIGN KEY (drain_id) REFERENCES drains (drain_id),
    CONSTRAINT fk_log_user  FOREIGN KEY (user_id)  REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상태 변경 이력';

-- ---------------------------------------------------------------------
-- 4) verifications: 제보자-청소자 교차 검증 결과
-- ---------------------------------------------------------------------
CREATE TABLE verifications (
    verify_id    BIGINT    NOT NULL AUTO_INCREMENT,
    drain_id     BIGINT    NOT NULL,
    reporter_id  BIGINT    NOT NULL,
    cleaner_id   BIGINT    NOT NULL,
    log_id       BIGINT    NOT NULL COMMENT '양호로 바꾼 이력 행',
    verified_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (verify_id),
    UNIQUE KEY uk_verify_log (log_id),
    KEY idx_verify_drain (drain_id),
    CONSTRAINT fk_verify_drain    FOREIGN KEY (drain_id)    REFERENCES drains (drain_id),
    CONSTRAINT fk_verify_reporter FOREIGN KEY (reporter_id) REFERENCES users (user_id),
    CONSTRAINT fk_verify_cleaner  FOREIGN KEY (cleaner_id)  REFERENCES users (user_id),
    CONSTRAINT fk_verify_log      FOREIGN KEY (log_id)      REFERENCES drain_status_log (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='교차 검증';

-- ---------------------------------------------------------------------
-- 5) mileage_ledger: 마일리지 원장 (잔액은 users.mileage_balance)
-- ---------------------------------------------------------------------
CREATE TABLE mileage_ledger (
    ledger_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    amount      INT          NOT NULL COMMENT '적립 +, 사용 -',
    reason      VARCHAR(30)  NOT NULL COMMENT '예: REPORT_VERIFIED, CLEAN_VERIFIED',
    verify_id   BIGINT       NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ledger_id),
    KEY idx_ledger_user_time (user_id, created_at),
    CONSTRAINT fk_ledger_user   FOREIGN KEY (user_id)   REFERENCES users (user_id),
    CONSTRAINT fk_ledger_verify FOREIGN KEY (verify_id) REFERENCES verifications (verify_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='마일리지 원장';

-- ---------------------------------------------------------------------
-- 6) hotspots: 군집화 결과와 위험도
--    분석을 다시 돌려도 기존 행을 지우지 않고 새로 추가합니다(같은 실행 = 같은 computed_at).
--    작업 지시(work_orders)가 옛 핫스팟을 가리켜도 깨지지 않게 하기 위함입니다.
-- ---------------------------------------------------------------------
CREATE TABLE hotspots (
    hotspot_id    BIGINT        NOT NULL AUTO_INCREMENT,
    center_lat    DECIMAL(10,7) NOT NULL,
    center_lng    DECIMAL(10,7) NOT NULL,
    radius_m      INT           NOT NULL COMMENT '지도에 그릴 원 반지름(중심~가장 먼 마커 + 여유)',
    drain_count   INT           NOT NULL,
    risk_score    DECIMAL(8,2)  NOT NULL,
    computed_at   DATETIME      NOT NULL,
    PRIMARY KEY (hotspot_id),
    KEY idx_hotspots_run (computed_at, risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='핫스팟';

-- ---------------------------------------------------------------------
-- 7) work_orders: 공무원의 청소 작업 지시
-- ---------------------------------------------------------------------
CREATE TABLE work_orders (
    order_id     BIGINT        NOT NULL AUTO_INCREMENT,
    hotspot_id   BIGINT        NOT NULL,
    official_id  BIGINT        NOT NULL,
    crew_id      BIGINT        NULL,
    status       ENUM('REQUESTED','IN_PROGRESS','DONE') NOT NULL DEFAULT 'REQUESTED',
    memo         VARCHAR(500)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_hotspot  FOREIGN KEY (hotspot_id)  REFERENCES hotspots (hotspot_id),
    CONSTRAINT fk_orders_official FOREIGN KEY (official_id) REFERENCES users (user_id),
    CONSTRAINT fk_orders_crew     FOREIGN KEY (crew_id)     REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='작업 지시';

-- ---------------------------------------------------------------------
-- 8) weather_forecast: 수집한 강수 예보
--    pcp: "강수없음" → 0, "1mm 미만" → 0.5, 범위 표기는 아래쪽 값 (4단계에서 변환)
--    source: KMA(기상청 수집) / MANUAL(시연용 직접 입력, 4단계)
-- ---------------------------------------------------------------------
CREATE TABLE weather_forecast (
    nx             SMALLINT      NOT NULL,
    ny             SMALLINT      NOT NULL,
    fcst_datetime  DATETIME      NOT NULL,
    pcp            DECIMAL(5,1)  NOT NULL DEFAULT 0 COMMENT '1시간 강수량 mm',
    pop            TINYINT UNSIGNED NULL COMMENT '강수확률 %',
    base_datetime  DATETIME      NULL COMMENT '발표 시각',
    source         ENUM('KMA','MANUAL') NOT NULL DEFAULT 'KMA',
    collected_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (nx, ny, fcst_datetime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='강수 예보';

-- ---------------------------------------------------------------------
-- 9) notifications: 앱 안 알림
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    noti_id     BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    type        VARCHAR(30)   NOT NULL COMMENT '예: CLEAN_DONE, RISK_ALERT, WORK_ORDER',
    message     VARCHAR(255)  NOT NULL,
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (noti_id),
    KEY idx_noti_user_unread (user_id, is_read, created_at),
    CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';

-- ---------------------------------------------------------------------
-- 행정동 기초 데이터 (시드가 아니라 고정 참조값이라 스키마와 함께 넣음)
-- 출처: 서울시 상권분석서비스 행정동 코드표(8자리) + '00'
-- 중심점은 시연 지역(왕십리·행당·마장·사근·용답·응봉) 동만 근사값으로 입력
-- ---------------------------------------------------------------------
INSERT INTO districts (district_code, name, center_lat, center_lng) VALUES
('1120052000', '왕십리2동',     37.5672000, 127.0282000),
('1120053500', '왕십리도선동',  37.5650000, 127.0335000),
('1120054000', '마장동',        37.5665000, 127.0430000),
('1120055000', '사근동',        37.5598000, 127.0470000),
('1120056000', '행당1동',       37.5603000, 127.0375000),
('1120057000', '행당2동',       37.5572000, 127.0310000),
('1120058000', '응봉동',        37.5520000, 127.0335000),
('1120059000', '금호1가동',     NULL, NULL),
('1120061500', '금호2·3가동',   NULL, NULL),
('1120062000', '금호4가동',     NULL, NULL),
('1120064500', '옥수동',        NULL, NULL),
('1120065000', '성수1가1동',    NULL, NULL),
('1120066000', '성수1가2동',    NULL, NULL),
('1120067000', '성수2가1동',    NULL, NULL),
('1120069000', '성수2가3동',    NULL, NULL),
('1120072000', '송정동',        NULL, NULL),
('1120079000', '용답동',        37.5650000, 127.0495000);

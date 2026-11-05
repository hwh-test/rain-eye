-- 레인아이 0단계: 데이터베이스와 전용 계정 만들기
-- HeidiSQL에서 root로 접속한 뒤 이 파일을 열어 전체 실행(F9)하세요.
-- 테이블 9개는 1단계(01_schema.sql)에서 만듭니다.

CREATE DATABASE IF NOT EXISTS raineye
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 웹앱 전용 계정 (root를 웹앱에서 쓰지 않습니다)
-- 비밀번호를 바꾸고, 같은 값을 config.properties의 db.password에 넣으세요.
CREATE USER IF NOT EXISTS 'raineye'@'localhost' IDENTIFIED BY 'change-me-please';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES,
      CREATE TEMPORARY TABLES, LOCK TABLES
  ON raineye.* TO 'raineye'@'localhost';
FLUSH PRIVILEGES;

-- 확인
USE raineye;
SELECT DATABASE() AS db, @@character_set_database AS charset, @@time_zone AS tz, NOW() AS now_kst;

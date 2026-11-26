-- =====================================================================
-- 레인아이 1단계 점검 쿼리 (읽기 전용 — 아무것도 바꾸지 않음)
-- HeidiSQL "raineye-app" 세션에서 실행해 웹앱 계정 권한도 함께 확인하세요.
-- 결과 탭이 여러 개 생기며, 각 쿼리 위 주석의 "정상" 값과 비교합니다.
-- =====================================================================
USE raineye;

-- 1) 테이블 10개(핵심 9 + districts) — 정상: 10행
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'raineye'
ORDER BY table_name;

-- 2) 상태별 개수 — 정상: DANGER 43 / CAUTION 63 / GOOD 220
SELECT status, COUNT(*) AS cnt FROM drains GROUP BY status;

-- 3) 계정 — 정상: 8명 (OFFICIAL 1, CREW 1, CITIZEN 6), 해시가 모두 $2a$10$ 로 시작
SELECT user_id, email, nickname, role, LEFT(password_hash, 7) AS hash_prefix, mileage_balance
FROM users ORDER BY user_id;

-- 4) 행정동별 마커 (P4 구역 통계의 원형) — 정상: 8개 동에 분포
SELECT d.name AS 행정동,
       SUM(dr.status = 'DANGER')  AS 위험,
       SUM(dr.status = 'CAUTION') AS 주의,
       SUM(dr.status = 'GOOD')    AS 양호,
       COUNT(*)                   AS 합계
FROM drains dr JOIN districts d ON d.district_code = dr.district_code
GROUP BY d.name ORDER BY 합계 DESC;

-- 5) 핫스팟 후보 중심 100m 안의 위험·주의 마커 수
--    정상: ① 28, ② 24, ③ 26, ④ 14, ⑤ 2
SELECT c.name AS 후보,
       COUNT(dr.drain_id) AS 위험주의_마커,
       SUM(CASE dr.status WHEN 'DANGER' THEN 3 WHEN 'CAUTION' THEN 1 ELSE 0 END) AS 점수합
FROM (
    SELECT '① 한양대역 앞 상권' AS name, 37.5591 AS lat, 127.0408 AS lng UNION ALL
    SELECT '② 왕십리역 주변',      37.5614, 127.0372 UNION ALL
    SELECT '③ 마장동 축산물시장',  37.5688, 127.0412 UNION ALL
    SELECT '④ 청계천 변 사근동',   37.5620, 127.0482 UNION ALL
    SELECT '⑤ 강의실(임시 좌표)',  37.5589, 127.0497
) c
LEFT JOIN drains dr
       ON dr.status IN ('DANGER', 'CAUTION')
      AND ST_Distance_Sphere(POINT(dr.lng, dr.lat), POINT(c.lng, c.lat)) <= 100
GROUP BY c.name ORDER BY c.name;

-- 6) 이력 정합성 — 정상: 0행 (모든 마커의 마지막 이력 상태 = 현재 상태)
SELECT dr.drain_id, dr.status, l.to_status
FROM drains dr
JOIN drain_status_log l ON l.log_id = (
    SELECT MAX(log_id) FROM drain_status_log WHERE drain_id = dr.drain_id)
WHERE l.to_status <> dr.status;

-- 7) 최근 제보 10건 (3단계 지도 화면에서 쓸 형태)
SELECT dr.drain_id, dr.status, dr.address, u.nickname AS 제보자, dr.updated_at
FROM drains dr JOIN users u ON u.user_id = dr.reporter_id
ORDER BY dr.updated_at DESC LIMIT 10;

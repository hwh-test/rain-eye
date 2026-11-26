"""
레인아이 1단계 시드 생성기
- 출력: sql/02_seed_demo.sql, tools/seed_preview.kml
- 실행: python tools/gen_seed.py   (BCrypt 해시는 PHP crypt()로 만들며, PHP가 없으면 기존 해시를 그대로 둠)
- 좌표·개수를 바꾸려면 아래 HOTSPOTS / AREA / 개수 상수를 고치고 다시 실행하세요.
모든 마커는 시연용 가상 데이터입니다.
"""
import math
import random
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RNG = random.Random(20261119)  # 고정 시드: 실행할 때마다 같은 좌표

# ---------------------------------------------------------------- 설정
DEMO_PASSWORD = "demo1234"

# (이름, 중심 위도, 중심 경도, 위험·주의 마커 수, 위험 비율, 설정 이유)
HOTSPOTS = [
    ("① 한양대역·한양대 앞 상권", 37.5591, 127.0408, 28, 0.45, "유동 인구, 담배꽁초·전단지"),
    ("② 왕십리역 주변 상가",       37.5614, 127.0372, 24, 0.40, "대형 환승역"),
    ("③ 마장동 축산물시장 일대",   37.5688, 127.0412, 26, 0.50, "시장 이물질"),
    ("④ 청계천 변 사근동(용답역 맞은편)", 37.5620, 127.0482, 14, 0.35, "하천 인근 저지대"),
]
CLUSTER_SIGMA_M = 35     # 핫스팟 안 흩어짐(표준편차)
CLUSTER_CLIP_M = 90      # 중심에서 최대 거리 (DBSCAN 반경 100m 안)

# ⑤ 발표 강의실 건물: 한양여자대학교 정보문화관 (2026-11-26 확정, Google 지도 위치 기준 근사 좌표)
LECTURE_HALL = ("⑤ 발표 강의실: 한양여대 정보문화관", 37.5586, 127.0490)
# ⑤ 주의 마커 2개: 정보문화관 서쪽 도로와 동쪽 도로 가장자리 (건물 안이 아니라 차도 옆)
LECTURE_SEEDS = [(37.5584901, 127.0487049), (37.5583741, 127.0494255)]

# 정보문화관 주변(반경 LOCAL_ZONE_R m)은 무작위 배치 대신 아래 차도 가장자리 위에만 마커를 둠.
# 2026-11-26 Google 내 지도 화면에서 따라 그린 근사 선(오차 약 5~10m). 건물 안·운동장·캠퍼스 광장에는 두지 않음
LOCAL_ZONE_R = 235
LOCAL_ROADS = {
    # (선 이름, 가장자리 여부): 가장자리=True면 선 위에, False(도로 중심선)면 양쪽 4m 옆에 배치
    ("살곶이길 서쪽 보도 쪽", True): [(37.5597703, 127.0502057), (37.5594524, 127.050287), (37.5591345, 127.0504441), (37.5587178, 127.0506283), (37.5585674, 127.0507096), (37.5580905, 127.0508938), (37.5576137, 127.0510347), (37.5571368, 127.0510726)],
    ("사근동10길(대각선)", False): [(37.5589283, 127.0487049), (37.5590958, 127.0489758), (37.5592676, 127.0492467)],
    ("사근동10길(북쪽)", False): [(37.5592762, 127.0492684), (37.5595125, 127.0492901), (37.5597703, 127.0493063)],
    ("사근동10길→살곶이길 연결로", False): [(37.5592934, 127.0492684), (37.5592934, 127.0497506), (37.5593622, 127.0503141)],
    ("캠퍼스 남쪽 도로", False): [(37.5571798, 127.0487808), (37.5572786, 127.0491925), (37.5573302, 127.0497072), (37.5573645, 127.0499944), (37.5573817, 127.050715), (37.5572356, 127.0508721), (37.5569349, 127.0509588)],
    ("정보문화관 서쪽 도로", False): [(37.5584815, 127.0474588), (37.5584901, 127.0480006), (37.5584815, 127.0486236), (37.5584901, 127.0487862)],
    ("행원파크 서쪽 도로", False): [(37.5579273, 127.0478597), (37.5580691, 127.0484611), (37.55788, 127.0485045), (37.5571798, 127.0487808)],
    ("전산관·교수회관 동쪽 도로", False): [(37.55846, 127.0494201), (37.5581808, 127.0494418), (37.5577941, 127.0494634), (37.5574504, 127.0494851)],
}

N_GOOD = 220             # 범위 전체에 흩뿌릴 양호 마커
N_NOISE_SINGLE = 8       # 단독 주의 마커 (DBSCAN 잡음)
N_NOISE_PAIR = 2         # 2개짜리 묶음 (최소 개수 3 미만 → 잡음)
NOISE_MIN_DIST_M = 250   # 잡음 마커와 핫스팟 중심/서로 간 최소 거리

# 시드 마커를 뿌릴 범위: 성동구 안쪽(청계천·중랑천·한강 쪽 가장자리는 여유를 두고 제외)
# (위도, 경도) 시계 방향. 지도에서 확인 후 조정하세요.
AREA = [
    (37.5685, 127.0290),  # 북서: 상왕십리 북쪽 (청계천 남쪽)
    (37.5692, 127.0360),
    (37.5702, 127.0385),  # 북: 마장 축산물시장 포함 (청계천 남쪽)
    (37.5702, 127.0435),
    (37.5690, 127.0452),
    (37.5688, 127.0478),  # 북동: 신답역 남쪽
    (37.5650, 127.0500),  # 답십리(동대문구) 쪽 제외
    (37.5638, 127.0518),  # 용답역 동쪽
    (37.5590, 127.0520),  # 한양여대 동쪽 (중랑천 서안 안쪽)
    (37.5560, 127.0492),
    (37.5537, 127.0440),  # 한양대 남쪽
    (37.5545, 127.0360),
    (37.5558, 127.0300),  # 행당역 남쪽
]

# 제외 구역: 청계천(하류)과 내부순환도로 띠. AREA 안이라도 여기에 들어가는 마커는 만들지 않음
# (2026-11-19 내 지도 화면 기준 근사 경계, 11-26 북쪽 서쪽 끝을 약 40m 넓혀 내부순환도로 포함. 서쪽 = 내부순환도로 서쪽 끝, 동쪽 = 2호선 성수지선 선로)
WATER = [
    (37.5697, 127.0455), (37.5639, 127.0480), (37.5614, 127.0495),
    (37.5595, 127.0506), (37.5575, 127.0512), (37.5540, 127.0525),
    (37.5540, 127.0535), (37.5582, 127.0521), (37.5601, 127.0516),
    (37.5621, 127.0510), (37.5639, 127.0502), (37.5697, 127.0483),
]

# 사용자: (user_id, email, nickname, role, district_code)
USERS = [
    (1, "official@raineye.test", "성동구청 담당자", "OFFICIAL", None),
    (2, "crew@raineye.test",     "현장기동반 1팀",  "CREW",     None),
    (3, "citizenb@raineye.test", "시민B",          "CITIZEN",  "1120055000"),
    (4, "resident1@raineye.test", "주민1", "CITIZEN", "1120056000"),
    (5, "resident2@raineye.test", "주민2", "CITIZEN", "1120053500"),
    (6, "resident3@raineye.test", "주민3", "CITIZEN", "1120054000"),
    (7, "resident4@raineye.test", "주민4", "CITIZEN", "1120055000"),
    (8, "resident5@raineye.test", "주민5", "CITIZEN", "1120079000"),
]
REPORTER_IDS = [4, 5, 6, 7, 8]

# 01_schema.sql의 districts 중심점과 같은 값 (가장 가까운 중심점으로 행정동 배정)
DISTRICT_CENTERS = {
    "1120052000": ("왕십리2동",    37.5672, 127.0282),
    "1120053500": ("왕십리도선동", 37.5650, 127.0335),
    "1120054000": ("마장동",       37.5665, 127.0430),
    "1120055000": ("사근동",       37.5598, 127.0470),
    "1120056000": ("행당1동",      37.5603, 127.0375),
    "1120057000": ("행당2동",      37.5572, 127.0310),
    "1120058000": ("응봉동",       37.5520, 127.0335),
    "1120079000": ("용답동",       37.5650, 127.0495),
}

# ---------------------------------------------------------------- 지오 유틸
R_EARTH = 6371000.0


def haversine(a, b):
    lat1, lng1 = map(math.radians, a)
    lat2, lng2 = map(math.radians, b)
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lng2 - lng1) / 2) ** 2
    return 2 * R_EARTH * math.asin(math.sqrt(h))


def offset(lat, lng, north_m, east_m):
    dlat = north_m / R_EARTH
    dlng = east_m / (R_EARTH * math.cos(math.radians(lat)))
    return lat + math.degrees(dlat), lng + math.degrees(dlng)


def in_poly(p, poly):
    lat, lng = p
    inside = False
    n = len(poly)
    for i in range(n):
        (y1, x1), (y2, x2) = poly[i], poly[(i + 1) % n]
        if (y1 > lat) != (y2 > lat):
            x = x1 + (lat - y1) * (x2 - x1) / (y2 - y1)
            if lng < x:
                inside = not inside
    return inside


def in_area(p):
    return in_poly(p, AREA) and not in_poly(p, WATER)


def nearest_district(p):
    return min(DISTRICT_CENTERS, key=lambda c: haversine(p, DISTRICT_CENTERS[c][1:]))


def random_in_area():
    lats = [a[0] for a in AREA]
    lngs = [a[1] for a in AREA]
    while True:
        p = (RNG.uniform(min(lats), max(lats)), RNG.uniform(min(lngs), max(lngs)))
        if in_area(p):
            return p


# ---------------------------------------------------------------- 마커 생성
drains = []  # dict(lat, lng, status, group)


def add(p, status, group):
    drains.append({"lat": p[0], "lng": p[1], "status": status, "group": group})


centers = [(h[1], h[2]) for h in HOTSPOTS] + [LECTURE_HALL[1:]]

for name, clat, clng, n, danger_ratio, _ in HOTSPOTS:
    n_danger = round(n * danger_ratio)
    statuses = ["DANGER"] * n_danger + ["CAUTION"] * (n - n_danger)
    RNG.shuffle(statuses)
    for st in statuses:
        while True:
            north, east = RNG.gauss(0, CLUSTER_SIGMA_M), RNG.gauss(0, CLUSTER_SIGMA_M)
            q = offset(clat, clng, north, east)
            if math.hypot(north, east) <= CLUSTER_CLIP_M and in_area(q):
                break
        add(q, st, name)

for p in LECTURE_SEEDS:
    add(p, "CAUTION", LECTURE_HALL[0])

noise_points = []


def far_enough(p):
    return all(haversine(p, c) >= NOISE_MIN_DIST_M for c in centers + noise_points)


while len(noise_points) < N_NOISE_SINGLE:
    p = random_in_area()
    if far_enough(p):
        noise_points.append(p)
        add(p, "CAUTION", "잡음(단독)")

for _ in range(N_NOISE_PAIR):
    while True:
        p = random_in_area()
        q = offset(p[0], p[1], RNG.uniform(-40, 40), RNG.uniform(-40, 40))
        if far_enough(p) and far_enough(q) and in_area(q):
            break
    noise_points += [p, q]
    add(p, "DANGER", "잡음(2개 묶음)")
    add(q, "CAUTION", "잡음(2개 묶음)")

good_points = []
while len(good_points) < N_GOOD:
    p = random_in_area()
    if all(haversine(p, g) >= 25 for g in good_points):
        good_points.append(p)
        add(p, "GOOD", "양호(흩뿌림)")

# ---------------------------------------------------------------- 정보문화관 주변: 차도 가장자리로 옮기기
def road_slots(step_m=12):
    slots = []
    for (name, is_edge), pts in LOCAL_ROADS.items():
        for (a, b) in zip(pts, pts[1:]):
            seg = haversine(a, b)
            n = max(1, int(seg // step_m))
            # 진행 방향에 수직인 단위 벡터 (북, 동 m)
            dn = (b[0] - a[0]) * 111320
            de = (b[1] - a[1]) * 111320 * math.cos(math.radians(a[0]))
            L = math.hypot(dn, de) or 1
            pn, pe = -de / L, dn / L
            for k in range(n):
                t = k / n
                base = (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
                sides = [0] if is_edge else [4, -4]
                for sd in sides:
                    slots.append((offset(base[0], base[1], pn * sd, pe * sd), name))
    return slots


LRNG = random.Random(1126)
zone_center = (LECTURE_HALL[1], LECTURE_HALL[2])
slots = [s_ for s_ in road_slots() if in_area(s_[0])]
LRNG.shuffle(slots)
taken = list(LECTURE_SEEDS)
moved = 0
for d in drains:
    p = (d["lat"], d["lng"])
    if d["group"] == LECTURE_HALL[0] or haversine(p, zone_center) > LOCAL_ZONE_R:
        continue
    risky = d["status"] != "GOOD"
    for i, (q, name) in enumerate(slots):
        if any(haversine(q, t) < 15 for t in taken):
            continue
        if risky and any(haversine(q, t) < 150 for t in LECTURE_SEEDS):
            continue
        d["lat"], d["lng"] = q
        d["group"] += " → 차도 옆(%s)" % name
        taken.append(q)
        slots.pop(i)
        moved += 1
        break
    else:
        raise SystemExit("정보문화관 주변 차도 자리가 모자랍니다")
print("[정보문화관 주변] 반경 %dm 안 마커 %d개를 차도 가장자리로 옮김" % (LOCAL_ZONE_R, moved))

# 검증: 모든 마커가 범위 안
assert all(in_area((d["lat"], d["lng"])) for d in drains), "범위 밖 마커가 있습니다"

# ---------------------------------------------------------------- DBSCAN 검증 (5단계 알고리즘과 같은 규칙)
EPS, MIN_PTS = 100.0, 3


def dbscan(points):
    labels = [None] * len(points)
    cid = 0
    for i in range(len(points)):
        if labels[i] is not None:
            continue
        nb = [j for j in range(len(points)) if haversine(points[i], points[j]) <= EPS]
        if len(nb) < MIN_PTS:
            labels[i] = -1
            continue
        cid += 1
        labels[i] = cid
        queue = [j for j in nb if j != i]
        while queue:
            j = queue.pop()
            if labels[j] == -1:
                labels[j] = cid
            if labels[j] is not None:
                continue
            labels[j] = cid
            nb2 = [k for k in range(len(points)) if haversine(points[j], points[k]) <= EPS]
            if len(nb2) >= MIN_PTS:
                queue += nb2
    return labels


def check_clusters(extra=None):
    risky = [d for d in drains if d["status"] != "GOOD"]
    pts = [(d["lat"], d["lng"]) for d in risky]
    sts = [d["status"] for d in risky]
    if extra:
        pts.append(extra)
        sts.append("CAUTION")
    labels = dbscan(pts)
    clusters = {}
    for lab, p, st in zip(labels, pts, sts):
        if lab > 0:
            clusters.setdefault(lab, []).append((p, st))
    return clusters, labels.count(-1)


def describe(clusters):
    rows = []
    for members in clusters.values():
        lat = sum(p[0] for p, _ in members) / len(members)
        lng = sum(p[1] for p, _ in members) / len(members)
        w = sum(3 if st == "DANGER" else 1 for _, st in members)
        near = min(centers, key=lambda c: haversine((lat, lng), c))
        name = ([h[0] for h in HOTSPOTS] + [LECTURE_HALL[0]])[centers.index(near)]
        rows.append((name, len(members), w))
    return sorted(rows)


clusters, noise = check_clusters()
print("[시드 상태] 핫스팟 %d개, 잡음 %d개" % (len(clusters), noise))
for r in describe(clusters):
    print("  %-28s 마커 %2d개  점수합 %3d (×R)" % r)
assert len(clusters) == 4, "시드 상태에서 핫스팟이 4개가 아닙니다"

# 시연: 시민 A가 강의실 중심에서 0~40m 어긋난 위치로 제보해도 ⑤가 생기는지
ok = 0
for k in range(50):
    ang, dist = RNG.uniform(0, 2 * math.pi), RNG.uniform(0, 40)
    a = offset(LECTURE_HALL[1], LECTURE_HALL[2], dist * math.cos(ang), dist * math.sin(ang))
    c, _ = check_clusters(a)
    ok += len(c) == 5
print("[시연 제보 후] 강의실 반경 40m 안 50회 모의 제보 중 핫스팟 5개가 된 경우: %d회" % ok)
assert ok == 50

# ---------------------------------------------------------------- BCrypt
def bcrypt_hash(pw):
    alphabet = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    salt = "".join(random.SystemRandom().choice(alphabet) for _ in range(21)) + RNG.choice(".Oeu")
    out = subprocess.run(["php", "-r", "echo crypt($argv[1], $argv[2]);", "--", pw, "$2a$10$" + salt],
                         capture_output=True, text=True, check=True).stdout
    assert out.startswith("$2a$10$") and len(out) == 60, out
    return out


pw_hash = bcrypt_hash(DEMO_PASSWORD)

# ---------------------------------------------------------------- SQL 출력
def q(s):
    return "NULL" if s is None else "'" + s.replace("'", "''") + "'"


RNG2 = random.Random(7)
MAX_AGE_MIN = 30 * 24 * 60
drain_rows, log_rows = [], []
log_id = 0
for i, d in enumerate(drains, start=1):
    code = nearest_district((d["lat"], d["lng"]))
    reporter = RNG2.choice(REPORTER_IDS)
    t0 = RNG2.randint(180, MAX_AGE_MIN)
    escalate = d["status"] == "DANGER" and RNG2.random() < 0.3
    t_last = RNG2.randint(30, t0 - 60) if escalate else t0
    address = "서울 성동구 %s 일대 (시연용 가상 데이터)" % DISTRICT_CENTERS[code][0]
    drain_rows.append("(%d, %.7f, %.7f, %s, '%s', '%s', %d, NOW() - INTERVAL %d MINUTE, NOW() - INTERVAL %d MINUTE)"
                      % (i, d["lat"], d["lng"], q(address), code, d["status"], reporter, t0, t_last))
    if escalate:
        log_id += 1
        log_rows.append("(%d, %d, %d, NULL, 'CAUTION', NULL, NOW() - INTERVAL %d MINUTE)" % (log_id, i, reporter, t0))
        log_id += 1
        other = RNG2.choice([r for r in REPORTER_IDS if r != reporter])
        log_rows.append("(%d, %d, %d, 'CAUTION', 'DANGER', NULL, NOW() - INTERVAL %d MINUTE)" % (log_id, i, other, t_last))
    else:
        log_id += 1
        log_rows.append("(%d, %d, %d, NULL, '%s', NULL, NOW() - INTERVAL %d MINUTE)" % (log_id, i, reporter, d["status"], t0))


def batched(prefix, rows, size=100):
    out = []
    for k in range(0, len(rows), size):
        out.append(prefix + "\n" + ",\n".join(rows[k:k + size]) + ";\n")
    return "\n".join(out)


counts = {s: sum(d["status"] == s for d in drains) for s in ("DANGER", "CAUTION", "GOOD")}
user_rows = ["(%d, %s, %s, %s, '%s', %s, 0, NOW() - INTERVAL 40 DAY)"
             % (uid, q(email), q(pw_hash), q(nick), role, q(dist)) for uid, email, nick, role, dist in USERS]

sql = f"""-- =====================================================================
-- 레인아이(Rain-I) 1단계: 시연용 시드 데이터  (tools/gen_seed.py가 생성 — 직접 고치지 말 것)
-- 실행: HeidiSQL "root" 세션에서 실행(F9). 01_schema.sql을 먼저 한 번 실행해 두어야 합니다.
-- **이 파일을 다시 실행하면 언제든 시드 상태로 되돌아갑니다(시드 되돌리기 겸용).**
--   districts(행정동)를 뺀 모든 테이블을 비우고 다시 채웁니다. 라이브 시연 중 가입한 계정·제보도 지워집니다.
-- 모든 마커는 실제 현장 조사가 아닌 시연용 가상 데이터입니다.
--
-- 마커 {len(drains)}개: 위험 {counts['DANGER']} / 주의 {counts['CAUTION']} / 양호 {counts['GOOD']}
--   핫스팟 후보 ①~④: 각 14~28개를 중심 90m 안에 배치 (DBSCAN 100m·3개 → 핫스팟 4개)
--   ⑤ 강의실 건물: 주의 2개 (시연 중 시민 A 제보로 3개째 → 핫스팟 5번째)
--   잡음: 단독 주의 {N_NOISE_SINGLE}개 + 2개 묶음 {N_NOISE_PAIR}쌍 (핫스팟으로 잡히지 않아야 정상)
-- 시연 계정 비밀번호(모두 같음): {DEMO_PASSWORD}
--   official@raineye.test (공무원) / crew@raineye.test (기동반) / citizenb@raineye.test (시민 B)
--   resident1~5@raineye.test (시드 마커를 제보한 가상 주민)
-- =====================================================================

USE raineye;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE mileage_ledger;
TRUNCATE TABLE verifications;
TRUNCATE TABLE drain_status_log;
TRUNCATE TABLE work_orders;
TRUNCATE TABLE hotspots;
TRUNCATE TABLE weather_forecast;
TRUNCATE TABLE drains;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

START TRANSACTION;

-- 계정 (user_id 1~8 고정, 시연 중 새로 가입하는 시민 A는 9번부터)
{batched("INSERT INTO users (user_id, email, password_hash, nickname, role, district_code, mileage_balance, created_at) VALUES", user_rows)}
-- 빗물받이 마커
{batched("INSERT INTO drains (drain_id, lat, lng, address, district_code, status, reporter_id, created_at, updated_at) VALUES", drain_rows)}
-- 상태 이력 (최초 제보 + 일부 위험 마커는 주의→위험 변경 이력)
{batched("INSERT INTO drain_status_log (log_id, drain_id, user_id, from_status, to_status, photo_path, created_at) VALUES", log_rows)}
COMMIT;

-- 확인: 아래 결과가 위험 {counts['DANGER']} / 주의 {counts['CAUTION']} / 양호 {counts['GOOD']} 이면 정상
SELECT status, COUNT(*) AS cnt FROM drains GROUP BY status;
"""
(ROOT / "sql" / "02_seed_demo.sql").write_text(sql, encoding="utf-8")

# ---------------------------------------------------------------- KML (Google 내 지도에서 경계 확인용)
STYLE = {"DANGER": "danger", "CAUTION": "caution", "GOOD": "good"}
COLOR = {"danger": "ff0000ff", "caution": "ff00aaff", "good": "ff00b050"}  # aabbggrr


def circle(lat, lng, r, n=36):
    pts = [offset(lat, lng, r * math.cos(2 * math.pi * k / n), r * math.sin(2 * math.pi * k / n)) for k in range(n + 1)]
    return " ".join("%.7f,%.7f,0" % (p[1], p[0]) for p in pts)


kml = ['<?xml version="1.0" encoding="UTF-8"?>',
       '<kml xmlns="http://www.opengis.net/kml/2.2"><Document><name>레인아이 시드 미리보기 (가상 데이터)</name>']
for sid, col in COLOR.items():
    kml.append(f'<Style id="{sid}"><IconStyle><color>{col}</color><scale>0.6</scale>'
               '<Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href></Icon></IconStyle></Style>')
kml.append('<Style id="area"><LineStyle><color>ffff0000</color><width>2</width></LineStyle><PolyStyle><color>15ff0000</color></PolyStyle></Style>')
kml.append('<Style id="hot"><LineStyle><color>ff0000ff</color><width>2</width></LineStyle><PolyStyle><color>300000ff</color></PolyStyle></Style>')
ring = " ".join("%.7f,%.7f,0" % (p[1], p[0]) for p in AREA + [AREA[0]])
hole = " ".join("%.7f,%.7f,0" % (p[1], p[0]) for p in WATER + [WATER[0]])
kml.append(f'<Placemark><name>시드 배치 범위 (가운데 빈 띠 = 청계천·내부순환도로 제외)</name><styleUrl>#area</styleUrl><Polygon><outerBoundaryIs><LinearRing><coordinates>{ring}</coordinates></LinearRing></outerBoundaryIs><innerBoundaryIs><LinearRing><coordinates>{hole}</coordinates></LinearRing></innerBoundaryIs></Polygon></Placemark>')
for name, lat, lng in [(h[0], h[1], h[2]) for h in HOTSPOTS] + [LECTURE_HALL]:
    kml.append(f'<Placemark><name>{name} (반경 100m)</name><styleUrl>#hot</styleUrl><Polygon><outerBoundaryIs><LinearRing><coordinates>{circle(lat, lng, 100)}</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>')
for i, d in enumerate(drains, start=1):
    kml.append(f'<Placemark><name>#{i} {d["status"]}</name><description>{d["group"]}</description>'
               f'<styleUrl>#{STYLE[d["status"]]}</styleUrl><Point><coordinates>{d["lng"]:.7f},{d["lat"]:.7f},0</coordinates></Point></Placemark>')
kml.append('</Document></kml>')
(ROOT / "tools" / "seed_preview.kml").write_text("\n".join(kml), encoding="utf-8")

print("마커 %d개 (위험 %d / 주의 %d / 양호 %d), 이력 %d행" % (len(drains), counts["DANGER"], counts["CAUTION"], counts["GOOD"], len(log_rows)))
print("행정동 분포:", {DISTRICT_CENTERS[c][0]: sum(nearest_district((d['lat'], d['lng'])) == c for d in drains) for c in DISTRICT_CENTERS})

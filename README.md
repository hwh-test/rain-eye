# 레인아이(Rain-I) — 0단계 개발 환경 구축

완료 기준: 로컬에서 `http://localhost:8080/rain-eye/` 가 뜨고, 점검표의 **DB 접속이 OK**.
덤으로 지도 테스트와 기상청 호출 테스트까지 통과하면 4단계까지 막힐 일이 줄어듭니다.

> **먼저 할 일 (오늘 바로):** 아래 6번의 기상청 API 2개 활용 신청과 7번의 Google Cloud 결제 계정 등록.
> 승인·키 활성화에 시간이 걸리니 설치를 기다리는 동안 해 두세요.

---

## 확정 버전

| 항목 | 버전 | 비고 |
| --- | --- | --- |
| JDK | 21 (LTS) | Temurin 등 아무 배포판 |
| Tomcat | 11.0.x 최신 | 최소 Java 17. 10.1도 동작(코드는 `jakarta.*` 기준) |
| MariaDB | 다운로드 페이지에서 **LTS** 표시된 최신 | 문자셋 utf8mb4 |
| JDBC 드라이버 | MariaDB Connector/J 3.5.10 | `WEB-INF/lib`에 직접 넣기 |
| IDE | Eclipse IDE for Enterprise Java and Web Developers | Dynamic Web Project |
| 패키지 | `kr.raineye.*` | |

## 폴더 구조

```
rain-eye/
├─ sql/00_create_db.sql              DB·전용 계정 생성
├─ src/main/java/
│  ├─ config.example.properties      → config.properties 로 복사해 값 채우기 (Git 제외)
│  └─ kr/raineye/
│     ├─ util/AppConfig.java         설정 읽기
│     ├─ util/DBUtil.java            DB 연결 (모든 DAO가 여기서 Connection 획득)
│     ├─ util/Json.java
│     └─ dev/                        0단계 점검용 — 2단계 이후 삭제
│        ├─ HealthServlet.java       GET /health
│        └─ KmaTestServlet.java      GET /dev/kma-test
└─ src/main/webapp/
   ├─ index.jsp                      환경 점검표
   ├─ maptest.jsp                    지도·Advanced Marker·현재 위치 테스트
   └─ WEB-INF/ web.xml, lib/
```

---

## 1. JDK 21

1. adoptium.net 에서 Temurin 21 설치(Windows는 .msi, 설치 중 "Set JAVA_HOME" 체크).
2. 확인: 터미널에서 `java -version` → `21.x`.

## 2. MariaDB + HeidiSQL

1. mariadb.org/download 에서 LTS 버전 MSI 설치.
   - root 비밀번호 설정, 포트 3306, **"Use UTF8 as default server's character set" 체크**.
   - Windows MSI에는 HeidiSQL이 함께 들어 있습니다(없으면 heidisql.com 에서 따로 설치).
2. HeidiSQL → 새 세션(127.0.0.1, root) 접속.
3. `sql/00_create_db.sql` 열기 → 비밀번호 `change-me-please`를 바꾸고 → 전체 실행(F9).
4. 마지막 SELECT 결과에서 charset이 `utf8mb4`인지 확인.

## 3. Tomcat 11 + Eclipse

1. tomcat.apache.org → Tomcat 11 → "64-bit Windows zip"을 받아 `C:\dev\apache-tomcat-11.0.xx` 같은 **공백 없는 경로**에 압축 해제.
2. Eclipse 실행 → Window › Preferences › Server › Runtime Environments › Add → Apache Tomcat v11.0 → 위 폴더, JRE는 21 선택.
3. File › New › Dynamic Web Project
   - 이름 `rain-eye`, Target runtime: Tomcat 11, Dynamic web module version: 6.0 이상
   - Content directory `src/main/webapp`, Java source `src/main/java` (최신 Eclipse 기본값)
4. 받은 압축 파일의 `src`, `sql`, `.gitignore`, `README.md`를 프로젝트 폴더에 덮어쓰기 → Eclipse에서 F5(새로고침).
5. `src/main/webapp/WEB-INF/lib/README.txt`의 링크에서 **mariadb-java-client-3.5.10.jar**(+ jbcrypt-0.4.jar)를 받아 그 폴더에 넣기.
6. `src/main/java/config.example.properties`를 복사해 **config.properties**로 이름 바꾸고 DB 비밀번호 입력.
7. 프로젝트 우클릭 › Run As › Run on Server → `http://localhost:8080/rain-eye/` 열기.

**0단계 완료:** 점검표의 "설정 파일 OK", "DB 접속 OK".

자주 막히는 곳
- 서블릿이 404 → Tomcat 9 이하를 골랐을 가능성. 10.1 이상이어야 `jakarta.servlet`이 동작합니다.
- `No suitable driver` → jar가 `WEB-INF/lib`에 없음.
- `Access denied for user 'raineye'` → SQL의 비밀번호와 config.properties 값이 다름.
- 한글 깨짐 → Eclipse › Preferences › General › Workspace › Text file encoding을 UTF-8로.

## 4. Git

1. git-scm.com 설치 → `git config --global user.name "황연지"` / `user.email`.
2. 프로젝트 폴더에서 `git init` → `git add .` → `git status`로 **config.properties가 목록에 없는지 꼭 확인** → 첫 커밋.
3. GitHub에 **Private** 저장소를 만들어 push. (API 키가 실수로 올라가면 즉시 키를 재발급하세요. 지운 커밋에도 남습니다.)

## 5. 폰에서 HTTPS로 테스트 (현재 위치·카메라용)

현재 위치는 HTTPS에서만 됩니다. 인증서 설정 없이 가장 빠른 방법은 Cloudflare 임시 터널입니다(계정 불필요).

1. `cloudflared` 설치 (Windows: `winget install --id Cloudflare.cloudflared`)
2. Tomcat이 켜진 상태에서: `cloudflared tunnel --url http://localhost:8080`
3. 출력된 `https://무작위이름.trycloudflare.com` 뒤에 `/rain-eye/maptest.jsp`를 붙여 폰에서 열기 → "내 위치" 버튼.

- 주소는 실행할 때마다 바뀝니다. 그래서 7번 리퍼러 제한에 `*.trycloudflare.com/*`을 넣어 둡니다.
- ngrok도 됩니다(가입·토큰 필요, 무료 고정 도메인 1개 제공). 발표 시연 땐 미리 터널을 켜 두고 주소를 QR로 띄우면 편합니다.

## 6. 기상청 API 활용 신청 (공공데이터포털)

1. data.go.kr 로그인 → 아래 두 개를 검색해 **활용신청**(개발계정, 목적: 캡스톤디자인 침수 위험 예측)
   - **기상청_단기예보 ((구)_동네예보) 조회서비스**
   - **기상청_기상특보 조회서비스**
2. 마이페이지 › 개발계정 › 인증키에서 **"일반 인증키 (Decoding)"** 값을 복사해 `kma.serviceKey`에 넣기.
   - Encoding 키를 넣으면 이중 인코딩되어 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`가 납니다.
   - 신청 직후에는 키가 아직 동기화되지 않아 같은 오류가 날 수 있습니다. 1~2시간 뒤 다시 시도.
3. `http://localhost:8080/rain-eye/dev/kma-test` → `"resultCode":"00"`과 `PCP`, `POP` 항목이 보이면 성공.
4. 기상특보 API는 승인 후 활용 명세(응답 필드)를 내려받아 `docs/`에 보관해 두세요. 4단계에서 씁니다.

## 7. Google Maps 키와 Map ID

1. console.cloud.google.com → 새 프로젝트 `rain-eye` → **결제 계정 연결**(카드 등록 필요, 개인 학습 규모는 월 무료 사용량 안에서 끝남).
2. API 및 서비스 › 라이브러리 › **Maps JavaScript API** 사용 설정.
3. 사용자 인증 정보 › API 키 만들기 → 키 제한:
   - 애플리케이션 제한: **웹사이트(HTTP 리퍼러)** — `http://localhost:8080/*`, `https://*.trycloudflare.com/*` (나중에 배포 주소 추가)
   - API 제한: Maps JavaScript API만
4. 결제 › 예산 및 알림에서 예산 알림(예: 1만 원)을 걸어 두면 안심됩니다.
5. 키를 `gmaps.apiKey`에 넣고 `maptest.jsp` 확인 → 색깔 마커 3개와 빨간 원이 보이면 성공.
6. (선택) Google Maps Platform › 지도 관리 › Map ID 만들기(JavaScript, 벡터) → `gmaps.mapId`를 `DEMO_MAP_ID`에서 교체.

> Advanced Marker는 **Map ID가 없으면 표시되지 않습니다.** 테스트는 `DEMO_MAP_ID`로 충분합니다.

---

## 0단계 체크리스트

- [ ] `java -version` 21
- [ ] HeidiSQL에서 `raineye` DB와 계정 생성, charset utf8mb4
- [ ] `localhost:8080/rain-eye/` 점검표 — 설정 파일 OK, DB 접속 OK
- [ ] Git 첫 커밋, config.properties 제외 확인, GitHub Private push
- [ ] 단기예보·기상특보 활용 신청 완료 (`/dev/kma-test` resultCode 00은 키 활성화 후)
- [ ] Maps 키 발급·리퍼러 제한, `maptest.jsp`에서 마커 표시
- [ ] 폰 + 터널 주소에서 "내 위치" 동작

다음 채팅: "프로젝트의 기준 문서를 읽고 1단계부터 시작하자." (DB 스키마 9개 테이블 + 시드 데이터)

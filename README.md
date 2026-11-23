# 🌊 Busan Play

부산의 청년과 외국인이 **문화행사·팝업·전시**를 찾아보고, 회차별로 참가 신청하는 웹 플랫폼입니다.

기업은 관리자 승인을 받아 행사를 열고, 회원은 포인트로 참가비를 낸 뒤 QR로 출석하고 후기와 커뮤니티로 소통합니다. 화면은 한국어·영어·일본어·중국어(간체) 4개 언어로 제공합니다.

| 항목 | 내용 |
| --- | --- |
| 기간 | 2026-10-30 – 2026-11-23 |
| 인원 | 5명 |
| 최종 코드 | `main` `1521d6f` (2026-11-23) |
| 문서 | [프로젝트 Wiki](https://github.com/JinwooLeeDev/event-platform/wiki) — 설계 문서 7종, 협업 매뉴얼, 통합 테스트 결과 |

---

## 화면

| 메인 | 행사 상세 |
| --- | --- |
| ![메인](https://raw.githubusercontent.com/wiki/JinwooLeeDev/event-platform/images/01-main.png) | ![행사 상세](https://raw.githubusercontent.com/wiki/JinwooLeeDev/event-platform/images/03-event-detail.png) |
| **출석 QR (기업)** | **회차 커뮤니티** |
| ![출석 QR](https://raw.githubusercontent.com/wiki/JinwooLeeDev/event-platform/images/14-attendance.png) | ![커뮤니티](https://raw.githubusercontent.com/wiki/JinwooLeeDev/event-platform/images/13-community.png) |

전체 화면 31종은 Wiki의 [화면 설계서](https://github.com/JinwooLeeDev/event-platform/wiki/화면-설계서)에 있습니다.

---

## 주요 기능

### 개인 회원
- 이메일 인증 회원가입, 로그인, 내 정보·선호 언어·비밀번호 변경
- 행사 목록·검색·상세 조회, 관심 행사
- 포인트 충전 (네이버페이·카카오페이, PortOne)
- 회차별 참가 신청: 정원이 남으면 **즉시 확정**, 가득 차면 **자동 대기** → 앞선 신청이 취소되면 자동 확정
- 신청 내역 조회, 본인 취소와 일정별 차등 환불
- 휴대폰으로 QR을 인식해 출석, 출석한 회차에 후기 작성·좋아요
- 확정된 회차의 실시간 커뮤니티 (WebSocket)

### 기업 회원
- 사업자 진위 확인(국세청 API)을 거친 기업 가입 신청 → 관리자 승인
- 행사 주최 신청(회차 함께 등록), 보완 요청 시 수정·재심사
- 회차별 보증금(150,000P) 결제 → 모든 회차를 납부하면 행사 공개
- 참가자 관리와 신청 거절(전액 환불), 출석 QR 띄우기, 커뮤니티 공지
- 회차 취소 신청, 포인트·보증금 내역

### 관리자
- 대시보드, 회원 상태 관리(정지·정지 해제)
- 기업 가입 심사, 행사 심사(승인·보완 요청)
- 회차 취소 승인·거절(참가자 전액 환불, 보증금 환불), 카테고리 관리

### 다국어
- 헤더의 `언어` 버튼과 첫 방문 언어 선택 창으로 4개 언어 전환. 개인 회원은 선호 언어를 저장
- 화면 문구, 오류·안내 문구, 인증 메일까지 선택한 언어로 표시
- **행사 내용 자동 번역 (선택 기능):** 공개 행사의 제목·요약·설명을 Gemini API로 번역해 보여 줍니다. 번역이 없으면 한국어 원문을 보여 줍니다.

### 설계에서 신경 쓴 부분
- **정원 동시성:** 회차 행을 비관적 잠금(`SELECT … FOR UPDATE`)으로 잠근 뒤 정원을 확인해, 동시에 신청해도 정원을 넘지 않습니다.
- **포인트 정합성:** 결제·환불 때 회원·기업 행을 잠그고, 결제·환불·신청 상태 변경을 한 트랜잭션에서 처리합니다.
- **권한:** 비회원·개인 회원·기업·관리자 4단계. 화면 숨김과 별개로 서버에서 역할과 소유권을 다시 확인합니다.

---

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어·프레임워크 | Java 21, Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Hibernate 7.4) |
| 화면 | Mustache 서버 렌더링, JavaScript(Fetch), WebSocket(STOMP) |
| DB | H2 (기본, 파일 DB), MySQL 8.0, Supabase PostgreSQL |
| 외부 연동 | PortOne(네이버페이·카카오페이), 국세청 사업자 진위 확인, 네이버 지도, SMTP 메일, Gemini API |
| 보안 | BCrypt 비밀번호 해시 (`spring-security-crypto`), 세션 로그인, 인터셉터 권한 검사 |
| 빌드 | Gradle |

---

## 실행 방법

### 1. 준비
- **Java 21**
- IntelliJ IDEA 권장
- 저장소를 내려받습니다.

```powershell
git clone https://github.com/JinwooLeeDev/event-platform.git
cd event-platform
```

> 저장소에 `gradle/wrapper/gradle-wrapper.jar`가 없어서 `gradlew.bat`이 바로 실행되지 않습니다. IntelliJ에서 Gradle 프로젝트로 열어 실행하거나, Gradle이 설치되어 있다면 `gradle wrapper`로 먼저 만들어 주세요.

### 2. 환경 변수 (`.env`)

프로젝트 루트의 `.env.example`을 `.env`로 복사해 값을 채웁니다. 서버가 시작할 때 `.env`를 자동으로 읽습니다. **`.env`는 Git에 올리지 않습니다.**

| 항목 | 용도 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `h2`(기본) / `mysql` / `supabase` |
| `MYSQL_DB_*`, `SUPABASE_DB_*` | 해당 프로필을 쓸 때 DB 접속 정보 |
| `MAIL_*` | 회원가입 인증 메일 (SMTP) |
| `NAVER_*`, `KAKAO_*` (`SECRET_KEY`, `STORE_ID`, `CHANNEL_KEY`) | PortOne 포인트 충전 |
| `NTS_SERVICE_KEY` | 국세청 사업자 진위 확인 |
| `NAVER_MAP_CLIENT_ID`, `NAVER_MAP_CLIENT_SECRET` | 행사 상세 지도 |
| `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TRANSLATION_WORKER_ENABLED` | 행사 자동 번역 (선택, 기본 꺼짐) |

결제·사업자 확인 키처럼 기본값이 없는 항목이 비어 있으면 서버가 시작되지 않을 수 있습니다. `.env.example`의 항목을 모두 넣어 주세요.

### 3. 실행

IntelliJ에서 `EventPlatformApplication`을 실행합니다. Gradle Wrapper가 준비되었다면 아래처럼 실행할 수도 있습니다.

```powershell
.\gradlew.bat bootRun
```

- 접속: http://localhost:8080
- 같은 Wi-Fi의 휴대폰에서 QR 출석을 시험하려면 PC의 IP 주소로 접속합니다 (예: `http://192.168.0.10:8080`).

### 4. DB와 기본 데이터

| 프로필 | DB | 비고 |
| --- | --- | --- |
| `h2` (기본) | 파일 DB `./data/event_platform` | 서버를 다시 켜도 데이터가 남습니다. 처음부터 하려면 서버를 끄고 `data/` 폴더를 지웁니다. |
| `mysql` | 로컬 MySQL 8.0 | `MYSQL_DB_URL` 등 설정 |
| `supabase` | Supabase PostgreSQL | Session pooler(5432) JDBC 주소 사용 |

- 세 프로필 모두 테이블은 JPA가 만들고(`ddl-auto: update`), 서버 시작 때 `db/demo-data.sql`이 **없는 데이터만** 넣습니다.
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/event_platform`, 사용자 `sa`, 비밀번호 빈 값)

| 테스트 계정 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 개인 회원 | `member@example.test` | `1234` |
| 관리자 (개인 로그인 사용) | `admin@example.test` | `1234` |
| 기업 (승인됨) | `company@example.test` | `1234` |

> ⚠️ 최종 main에서는 가입용 비밀번호 규칙이 로그인에도 적용되어 위 계정으로 로그인할 수 없습니다 (Wiki 작업 분담 문서의 **T-20**). 수정되기 전에는 회원가입으로 새 계정을 만들어 사용하세요. 새 비밀번호는 영문·숫자·특수문자(`$@!%*#?&`)를 포함한 8–16자여야 합니다.

### 5. 행사 자동 번역 켜기 (선택)

1. DB에서 `src/main/resources/db/manual/event-translation.sql`을 실행해 번역 테이블 3개를 만듭니다. 자동으로 실행되지 않습니다.
2. `.env`에 `GEMINI_API_KEY`, `GEMINI_MODEL`을 넣고 `GEMINI_TRANSLATION_WORKER_ENABLED=true`로 바꿉니다.
3. 영어는 행사가 공개·수정될 때, 일본어·중국어는 그 언어로 처음 볼 때 번역을 요청합니다. 분당 5회·하루 100회로 제한합니다.

---

## 프로젝트 구조

```text
src/main/java/com/busan/eventplatform
├── admin          관리자 화면 (대시보드, 회원·행사·회차·취소·카테고리)
├── api            포인트 충전(payment), 참가비·보증금 결제(purchase), 환불(refund)
├── company        기업 정보, 사업자 진위 확인, 기업 가입 심사
├── event          행사, 회차, 카테고리, 대표 이미지, 관심 행사, 후기, 회차 취소, 번역(translation)
├── login          개인·기업 로그인과 가입, 이메일 인증, 마이페이지
├── user           참가 신청(registration), 출석(attendance), 커뮤니티(community)
├── global         예외 처리, 다국어(i18n), 인터셉터·보안, 파일, 공통 응답
└── main           메인 화면

src/main/resources
├── templates      Mustache 화면 80개
├── static         CSS, JavaScript, 이미지
├── messages*.properties   화면 문구 (ko, en, ja, zh_CN)
└── db             demo-data.sql (기본 데이터), manual/ (번역 테이블)
```

---

## 팀

| 담당 | 주요 역할 |
| --- | --- |
| 조장 (`I3abel` / `JinwooLeeDev`) | 공통 예외·인터셉터, 회원가입·로그인·마이페이지, 전체 화면 디자인, 관리자 화면, 다국어 지원, PR 검토·Merge |
| `MIKY-0` | 공통 엔티티 설계, 포인트 충전, 참가비·보증금 결제, 환불, 회차 자동 상태 변경 |
| 윤아 | 행사 등록·수정, 회차, 카테고리, 대표 이미지, 회차 취소 신청, 후기 |
| `dalyume2876` | 기업 가입·정보 수정, 사업자 진위 확인, 기업 심사, 보증금 상태 표시, 포인트·보증금 내역 |
| 김도현 (`Sonice-lab`) | 참가 신청·정원 동시성·대기자 승격, 신청 내역, 커뮤니티, 기업 거절, QR 출석, 통합 테스트, 설계 문서·Wiki |

자세한 작업 목록과 PR 기록은 Wiki의 [작업 분담 및 Issue 목록](https://github.com/JinwooLeeDev/event-platform/wiki/작업-분담-및-Issue-목록)에 있습니다.

---

## 문서

| 문서 | 내용 |
| --- | --- |
| [요구사항 명세서](https://github.com/JinwooLeeDev/event-platform/wiki/요구사항-명세서) | 기능·보안·데이터·비기능 요구사항 |
| [권한별 기능 매트릭스](https://github.com/JinwooLeeDev/event-platform/wiki/권한별-기능-매트릭스) | 사용자별 기능 허용 범위 |
| [데이터베이스 설계서 및 ERD](https://github.com/JinwooLeeDev/event-platform/wiki/데이터베이스-설계서-및-ERD) | 테이블 24개(번역 3개 포함), ERD, 잠금·트랜잭션 |
| [사용자 시나리오 및 업무 규칙](https://github.com/JinwooLeeDev/event-platform/wiki/사용자-시나리오-및-업무-규칙) | 사용자별 정상·예외 흐름 |
| [화면 설계서](https://github.com/JinwooLeeDev/event-platform/wiki/화면-설계서) | 화면 31종의 구성과 동작 |
| [통합 테스트 결과](https://github.com/JinwooLeeDev/event-platform/wiki/통합-테스트-결과) | 테스트 결과, 발견 문제, 최종 main 반영 상태 |
| [작업 순서](https://github.com/JinwooLeeDev/event-platform/wiki/작업-순서) | Issue → 브랜치 → PR → 리뷰 → Merge 협업 규칙 |

---

## 남은 과제

통합 테스트에서 찾은 문제 중 아직 남은 주요 항목입니다. 전체 목록은 [통합 테스트 결과](https://github.com/JinwooLeeDev/event-platform/wiki/통합-테스트-결과)에 있습니다.

- 🔴 **T-20** 로그인에 가입용 비밀번호 규칙이 적용되어 기존 계정 로그인 불가
- 🔴 **T-17** 기업 세션이 없을 때 회차 관리 목록 화면 500
- 🔴 **T-12** 서버 재시작 후 회차 "진행 중" 자동 전환이 사라짐
- 🔴 **T-1·T-2** 첫 로그인 시 `;jsessionid` 주소 404, 회차 5개 이상 행사 등록 시 413
- 행사 상세 지도의 주소 검색 실패 (네이버 클라우드 Geocoding 설정 확인 필요)

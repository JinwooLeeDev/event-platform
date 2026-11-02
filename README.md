# Event Platform

팀 개발 시 [Git 협업 규칙](CONTRIBUTING.md)의 브랜치·커밋·PR 작성 기준을 따릅니다.

부산 청년·외국인 문화행사 플랫폼의 Spring Boot 기본 프로젝트입니다.
역할 분담안을 기준으로 패키지 구조와 로컬 DB 자동 초기화를 준비했으며, 업무 기능은 아직 구현하지 않았습니다.

- Java 21, Spring Boot 4.1.2, Gradle Wrapper
- Spring MVC, Spring Data JPA, Bean Validation
- H2 / MySQL / PostgreSQL(Supabase) JDBC 드라이버

## 패키지 구조와 담당 영역

기본 패키지는 `com.busan.eventplatform`입니다.
각 도메인 아래에는 `controller`, `service`, `repository`, `entity`, `dto` 패키지를 두었습니다.
빈 패키지는 `.gitkeep`으로 Git에 유지하며, 실제 클래스를 추가하면 해당 파일을 삭제해도 됩니다.

| 담당 | 패키지 (`domain` 하위) |
|---|---|
| 조장: 회원·인증·이메일 인증·파일 | `user`, `auth`, `emailverification`, `file` |
| 부조장: 결제·보증금·환불·알림 | `payment`, `deposit`, `refund`, `notification` |
| 팀원 1: 행사·카테고리·행사 첨부파일 | `event`, `eventcategory` (행사 첨부파일은 `event`에서 공통 `file` 연동) |
| 팀원 2: 참가 신청·출석·상태 이력 | `registration`, `attendance` (상태 이력은 해당 도메인에서 관리) |
| 팀원 3: 기업·사업자 검증·관리자 심사 | `company`, `businessverification`, `adminreview` |

조장이 관리하는 공통 영역은 `global.config`, `global.security`, `global.exception`, `global.response`, `global.util`입니다.
API, 시큐리티 필터, 외부 연동 및 비즈니스 로직은 구현하지 않았습니다. MySQL 개발용 더미 데이터는 제공됩니다.

## 실행 및 빌드

Windows PowerShell에서 실행합니다. Java 21이 필요하며 Gradle은 별도 설치하지 않아도 됩니다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test build --console=plain
```

기본 포트는 8080입니다. 아직 API가 없어 `/` 요청은 404가 정상입니다.

## 기본 개발 DB: H2

`.env`나 실행 환경에 활성 프로필을 지정하지 않으면 `h2` 프로필로 실행합니다.
메모리 DB이므로 앱을 종료하면 데이터가 사라지고, 시작 시 엔티티로 테이블을 생성합니다.

- 콘솔: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:event_platform`
- 사용자: `sa`, 비밀번호: 빈 값

## 로컬 MySQL

MySQL 8.0+ 서버를 실행하고, `.env.example`을 루트의 `.env`로 복사해 다음 값을 작성하세요.
앱이 `.env`를 자동으로 읽으므로 IDE의 환경 변수 파일 플러그인이 필요하지 않습니다.

```dotenv
SPRING_PROFILES_ACTIVE=mysql
MYSQL_DB_URL=jdbc:mysql://localhost:3306/event_platform?connectionTimeZone=UTC&characterEncoding=UTF-8
MYSQL_DB_USERNAME=root
MYSQL_DB_PASSWORD=your-local-password
```

```powershell
.\gradlew.bat bootRun
```

기본 접속 주소는 `localhost:3306/event_platform`입니다.
최초 서버 실행 시 DB·테이블 27개·외래키 60개와 더미 데이터를 자동 생성한 뒤 JPA를 시작합니다.
다음 실행부터는 완료 기록을 확인해 초기화를 건너뛰므로 데이터가 중복되거나 기존 수정 내용이 덮어써지지 않습니다.
Python이나 MySQL CLI는 서버 자동 초기화에 필요하지 않습니다. Supabase·H2에는 이 초기화를 실행하지 않습니다.
로컬 주소만 허용하며 DB 이름은 `event_platform`, `event_platform_dev`, `event_platform_test` 중 하나를 사용합니다.
초기화 기록 없는 기존 테이블이 있으면 중단합니다. 자세한 내용은 [로컬 MySQL 안내](db/local-mysql/README.md)를 참고하세요.

## Supabase PostgreSQL

Supabase 프로젝트를 만든 뒤 Dashboard의 **Connect → Session pooler**에서 호스트와 사용자 이름을 확인합니다.
IPv4 환경에서도 사용할 수 있는 Session pooler(5432)를 기준으로 설정했습니다.
REST API 주소나 anon/service role 키가 아닌 **DB 비밀번호와 JDBC 접속 정보**가 필요합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'supabase'
$env:SUPABASE_DB_URL = 'jdbc:postgresql://YOUR-POOLER-HOST:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USERNAME = 'postgres.YOUR_PROJECT_REF'
$env:SUPABASE_DB_PASSWORD = 'your-db-password'
.\gradlew.bat bootRun
```

SSL을 사용하며 커넥션 풀은 최대 5개입니다. 프로젝트의 연결 한도에 맞춰 조정하세요.
`ddl-auto=validate`로 기존 스키마만 검증하며 테이블을 자동 생성·수정하지 않습니다.
엔티티를 추가할 때는 Supabase에 해당 스키마를 먼저 적용해야 합니다. 마이그레이션 도구는 아직 추가하지 않았습니다.

H2·MySQL·PostgreSQL은 SQL과 자료형이 다르므로 실제 배포 전에 Supabase에서도 쿼리·스키마·트랜잭션을 검증하세요.
이 설정은 DB 연결만 다루며 Supabase Auth·Storage 연동은 포함하지 않습니다.

프로필은 한 번에 하나만 선택하세요. H2로 돌아가려면 `SPRING_PROFILES_ACTIVE`를 `h2`로 설정합니다.
프로젝트 루트(실행 작업 디렉터리)의 `.env`를 자동으로 읽습니다. OS·IDE 환경 변수와 명령행 설정이 `.env`보다 우선합니다.
비밀번호가 포함된 `.env`는 Git에 올리지 마세요. `.env.example`에는 예시 값만 유지합니다.

공식 참고: [Supabase Spring Boot 연결](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot), [DB 접속 방식](https://supabase.com/docs/guides/database/connecting-to-postgres)

# Event Platform

부산 청년·외국인 문화행사 플랫폼의 Spring Boot 기본 프로젝트입니다.
역할 분담안을 기준으로 패키지 구조를 준비했으며, 업무 기능과 클래스는 아직 구현하지 않았습니다.

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
API, 시큐리티 필터, 외부 연동, 더미 데이터 및 비즈니스 로직은 구현하지 않았습니다.

## 실행 및 빌드

Windows PowerShell에서 실행합니다. Java 21이 필요하며 Gradle은 별도 설치하지 않아도 됩니다.

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test build --console=plain
```

기본 포트는 8080입니다. 아직 API가 없어 `/` 요청은 404가 정상입니다.

## 기본 개발 DB: H2

환경 변수를 지정하지 않으면 `h2` 프로필로 실행합니다.
메모리 DB이므로 앱을 종료하면 데이터가 사라지고, 시작 시 엔티티로 테이블을 생성합니다.

- 콘솔: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:event_platform`
- 사용자: `sa`, 비밀번호: 빈 값

## 로컬 MySQL

MySQL 서버를 별도로 실행하고 `event_platform` 데이터베이스를 생성하세요.

```sql
CREATE DATABASE event_platform CHARACTER SET utf8mb4;
```

```powershell
$env:SPRING_PROFILES_ACTIVE = 'mysql'
$env:MYSQL_DB_USERNAME = 'root'
$env:MYSQL_DB_PASSWORD = 'your-local-password'
.\gradlew.bat bootRun
```

기본 접속 주소는 `localhost:3306/event_platform`입니다.
다른 주소는 `MYSQL_DB_URL`로 지정합니다. 개발 편의를 위해 `ddl-auto=update`를 사용합니다.

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
`.env.example`은 환경 변수 참고용이며 Spring Boot가 `.env` 파일을 자동으로 읽지는 않습니다.
비밀번호는 PowerShell 또는 IDE 실행 설정의 환경 변수로 전달하고 Git에 올리지 마세요.

공식 참고: [Supabase Spring Boot 연결](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot), [DB 접속 방식](https://supabase.com/docs/guides/database/connecting-to-postgres)

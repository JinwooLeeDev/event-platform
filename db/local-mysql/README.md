# 로컬 MySQL 개발 데이터

제공된 2026-11-02 MySQL 모델을 기준으로 **업무 테이블 27개, 외래키 60개**를 구성합니다.
컬럼의 타입·NULL 허용·기본값·PK·유일 인덱스·FK를 유지했습니다.
별도로 `_local_dev_bootstrap` 테이블 1개를 초기화 버전·SQL 해시·완료 상태 기록에 사용합니다.
교차 행 원장 균형, 상태 전이, 소유권 등의 업무 제약을 강제하는 트리거나 기능 코드는 추가하지 않았습니다.

## 최초 서버 실행 시 자동 초기화

Java 21과 실행 중인 로컬 MySQL 8.0+가 필요합니다.
서버 자동 초기화에는 Python과 MySQL CLI가 필요하지 않습니다.
DB 계정에는 로컬 데이터베이스·테이블 생성 및 데이터 삽입 권한이 있어야 합니다.

루트 `.env`에 로컬 접속 정보를 작성합니다. 비밀번호에 특수문자가 있어도 원문 그대로 입력합니다.

```dotenv
SPRING_PROFILES_ACTIVE=mysql
MYSQL_DB_URL=jdbc:mysql://localhost:3306/event_platform?connectionTimeZone=UTC&characterEncoding=UTF-8
MYSQL_DB_USERNAME=root
MYSQL_DB_PASSWORD=your-local-password
```

프로젝트 루트에서 실행합니다.

```powershell
.\gradlew.bat bootRun
```

- 서버는 작업 디렉터리의 `.env`를 자동으로 읽으며, `mysql` 프로필에서만 로컬 DB를 초기화합니다.
- `supabase`, `h2`에서는 초기화 설정을 등록하지 않습니다. `mysql`을 다른 DB 프로필과 동시에 선택하면 연결 전에 중단합니다.
- `localhost`, `127.0.0.1`, `::1`의 MySQL만 허용하고 PostgreSQL·원격 호스트 주소는 거부합니다.
- DB 이름은 `event_platform`, `event_platform_dev`, `event_platform_test`만 허용합니다.
- 대상 DB가 없으면 생성합니다. 초기화 기록 없이 기존 테이블이 있으면 중단하며 삭제하거나 덮어쓰지 않습니다.
- 기존 로컬 DB를 보존해야 하면 `.env`의 DB 이름을 `event_platform_dev`로 바꾸어 새 DB에 초기화합니다.
- 최초 실행은 스키마를 생성한 뒤, 더미 데이터와 완료 기록을 한 트랜잭션으로 넣습니다.
- 완료 후 서버를 재실행하면 `Local MySQL already initialized` 로그를 출력하고 스키마·데이터 실행을 건너뜁니다.
- 매번 전체 테이블과 원장을 검사하지 않습니다. 전체 검증은 필요할 때 별도 명령으로 실행합니다.
- 이전 Python 스크립트로 초기화한 DB의 완료 기록과 해시도 그대로 인식합니다.
- 데이터 삽입 실패 시 삽입 트랜잭션은 롤백됩니다. MySQL DDL은 자동 커밋되므로 테이블이 남을 수 있고, 같은 SQL로 재실행하면 이어서 처리합니다.
- 같은 DB의 동시 초기화는 MySQL named lock과 상태 재확인으로 중복 삽입을 막습니다.
- 초기화한 SQL 파일이 바뀌면 해시 불일치로 중단합니다. 데이터 갱신은 검토한 마이그레이션 또는 새 로컬 DB를 사용하세요.

SQL 원본은 `src/main/resources/db/local-mysql/`에 있으며 실행 JAR에도 포함됩니다.
`01-schema.sql`과 `02-seed.sql`을 개별 실행하면 위 안전 검사와 한 번만 실행하는 동작이 적용되지 않습니다.
서버 자동 초기화 또는 선택적 수동 스크립트를 사용하세요. 자동 초기화는 DROP·TRUNCATE·외래키 검사 해제·운영 접속을 실행하지 않습니다.

## 앱 실행

`mysql` 프로필에서는 앱의 DataSource를 만들기 전에 전용 초기화 코드가 스키마와 더미 데이터를 준비합니다.
이후 Hibernate는 `ddl-auto=validate`로 동작합니다.
`spring.sql.init.mode=never`는 Spring의 일반 SQL 초기화가 중복 실행되는 것을 막는 설정이며, 전용 초기화 코드와는 별개입니다.
MySQL 세션과 Hibernate의 시간대는 UTC입니다.

IDE에서도 프로젝트 루트를 Working directory로 설정하고 메인 클래스를 실행하면 됩니다.
`.env` 안의 `SPRING_PROFILES_ACTIVE`를 `mysql`로 작성하세요.
OS·IDE 환경 변수와 `--spring.profiles.active` 등 명령행 설정은 `.env`보다 우선하므로 기존 `supabase` 설정이 남지 않았는지 확인하세요.
비밀번호는 따옴표로 감쌀 수 있고, 따옴표 안의 `#`, `$`, `!`, 역슬래시는 그대로 유지합니다.
`.env`는 여러 줄 값이나 변수 치환을 지원하지 않습니다.

## 데이터 구성

| 영역 | 초기 데이터 |
|---|---|
| 사용자 | 관리자 1명, 기업 계정 3명, 개인회원 10명 |
| 기업 | 승인된 가상 기업 3개, 가상 사업자번호 |
| 행사 | 진행 중 유료 팝업, 완료된 무료 전시, 유료 특강, 무료 워크숍, 취소 행사 총 5개·5회차 |
| 심사·신청 | 심사 5건, 신청 10건, 상태 이력 19건. HOLD·WAITING·CONFIRMED·EXPIRED·CANCELLED 예시 |
| 참여 | 출석 4건, 후기 2건, 채팅·공지 3건 |
| 지갑·원장 | 지갑 14개, 계정 18개, 거래 30건, 증감 원장 60건. 계정 잔액은 원장에서 계산 |
| 결제·정산 | 가상 충전 14건, 보증금 5건, 참가비 차감 4건, 보상 1건, 대기 정산 1건, 반환 3건 |
| 상품 | 상품 2개, 주문 2건, 광고 1건 |
| 공통 | 알림 3건, 감사 이력 2건, 파일 메타데이터 3건, 이메일 인증 이력 14건 |

이메일은 `admin@example.test`, `company1@example.test`~`company3@example.test`,
`member01@example.test`~`member10@example.test`입니다.
회원 ID는 관리자 1, 기업 계정 2~4, 개인회원 5~14이며 지갑 ID는 회원 ID와 같습니다.

비밀번호 필드는 `LocalDemo123!`의 PBKDF2-HMAC-SHA256 해시를 더미 포맷으로 보관합니다.
현재 인증 API와 해당 해시 포맷을 읽는 PasswordEncoder는 없으므로 이 계정으로 로그인이 구현된 것은 아닙니다.
인증 개발 시 팀의 인코더에 맞춰 개발 계정 해시를 별도로 준비하세요.

결제 provider는 `LOCAL_MOCK`이며 실제 PG 요청·메일 발송·스토리지 업로드는 수행하지 않습니다.
알림 EMAIL 예시는 이미 `SENT`이고, 파일 경로는 실물이 없는 메타데이터입니다.
가상 사업자번호는 외부 진위 확인에 사용할 수 없습니다.

행사 시각은 최초 초기화 시점을 기준으로 생성되고 재시작 시 바뀌지 않습니다.
따라서 시간이 지나면 HOLD 만료·광고 종료 등의 시각이 과거가 됩니다. 워커 구현 후에는 상태가 바뀔 수 있습니다.
원장의 충전·차감은 현재 표시된 더미 상태에 맞춘 예시이며 정책 미정값을 확정하는 근거가 아닙니다.

## 검증

아래 선택적 수동 명령에는 Python 3.10+와 MySQL CLI가 필요합니다. 서버 실행에는 필요하지 않습니다.

```powershell
# DB 변경 없이 검증
python scripts/init-local-mysql.py --verify-only

# 원격 호스트 차단 및 .env 특수문자 파싱 테스트
python -m unittest discover -s scripts -p 'test_init_local_mysql.py'

# 서버 없이 수동 초기화 (서버와 같은 완료 기록 사용, 이미 완료됐다면 건너뜀)
python scripts/init-local-mysql.py
```

검증 SQL은 테이블·FK 수, 거래 합계 0, 원장과 잔액 캐시 일치, 음수 잔액,
신청 정원·순번·기간, 출석 시도, 후기 작성 기간, 반환 원거래·한도,
보증금 반환·귀속 한도, 정산·상품 금액, 파일 연결 수, 보상 소유권을 확인합니다.
`FAIL`이 나오면 명령은 실패로 종료합니다. 이 검증은 초기 모델 기준이며 실제 업무 로직 테스트를 대신하지 않습니다.

일반 `gradlew test`는 H2와 단위 테스트만 실행합니다.
실제 MySQL 최초 실행·재실행 통합 테스트는 다음처럼 선택적으로 실행합니다.
이 테스트는 `event_platform_test` DB가 이미 있으면 중단하고, 없을 때만 해당 DB를 만들었다가 테스트 후 삭제합니다.
개발 중인 `event_platform`과 `event_platform_dev`는 삭제하지 않습니다.

```powershell
$env:LOCAL_MYSQL_BOOTSTRAP_TEST = 'true'
.\gradlew.bat test --rerun-tasks --console=plain
Remove-Item Env:LOCAL_MYSQL_BOOTSTRAP_TEST
```

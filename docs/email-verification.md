# 이메일 인증 사용법

세션 기반으로 인증 정보를 관리합니다. 인증번호 발송·확인·회원가입은 같은 브라우저 세션에서 진행해야 합니다. User 엔티티와 직접적인 연관관계는 없습니다.

## 1. 화면에서 인증번호 발송

Mustache 화면의 JavaScript에서 호출합니다. 아래 `email`은 사용자가 입력한 이메일입니다.

```javascript
const response = await fetch("/api/v1/email-verifications", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ email })
});

const result = await response.json();
// response.ok로 성공 여부를 확인하고 result.message를 화면에 표시
```

발송 성공 응답은 HTTP 200, `{ "message": "인증번호가 발송되었습니다. 5분 안에 입력해주세요.", "verified": false }`입니다. 발송 성공의 `verified: false`는 아직 인증번호 확인 전이라는 뜻입니다.

## 2. 화면에서 인증번호 확인

`code`는 사용자가 입력한 6자리 문자열입니다.

```javascript
const response = await fetch("/api/v1/email-verifications/confirm", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ email, code })
});

const result = await response.json();
if (response.ok && result.verified) {
    // 인증 완료 표시 및 이메일 입력 잠금
}
// result.message를 화면에 표시
```

인증 성공 응답은 HTTP 200, `{ "message": "이메일 인증이 완료되었습니다.", "verified": true }`입니다. 입력·검증 오류는 HTTP 400, 메일 발송 실패는 HTTP 500과 JSON 안내 메시지를 반환합니다. 이 응답 규칙은 현재 Controller의 Exception400·Exception500 처리에 해당합니다.

## 3. 서버 내부에서 호출

Controller나 Service에서 EmailVerificationService를 생성자 주입받습니다.

```java
import com.busan.eventplatform.emailverification.EmailVerificationService;

// 클래스에 @RequiredArgsConstructor 적용
private final EmailVerificationService emailVerificationService;
```

```java
emailVerificationService.sendCode(email, session);
emailVerificationService.confirmCode(email, code, session);
```

`session`은 해당 요청의 `HttpSession`입니다. 직접 호출할 때도 요청 DTO의 `validate()` 등으로 이메일·코드 형식을 먼저 검증해야 합니다. 이메일 앞뒤 공백 제거 기준도 일치시킵니다.

## 4. 회원가입에 연결

회원가입 요청에서 가입할 이메일의 인증 완료 여부를 서버에서 확인합니다.

```java
emailVerificationService.requireVerifiedEmail(email, session);

// 회원가입 Service 호출: 이메일 중복 검사 및 회원 저장
// 회원 저장 트랜잭션이 정상 커밋된 뒤 인증 정보 제거
session.removeAttribute(Define.EMAIL_VERIFICATION);
```

```java
import com.busan.eventplatform.global.util.Define;
```

`requireVerifiedEmail()`은 인증 여부·이메일 일치·인증 완료 유효기간을 확인하며, 인증 정보를 소비하지는 않습니다. 가입 실패 시에는 인증 정보를 제거하지 않습니다. 화면의 `verified` 값만으로 가입을 허용하면 안 됩니다.

현재 확인과 제거는 별도 동작이므로 동시 회원가입까지 원자적으로 처리하지는 않습니다. 회원가입 담당자는 DB 이메일 유니크 제약과 중복 요청 처리를 함께 구현해야 합니다.

## 5. 적용 기준

| 항목 | 현재 값 |
|---|---|
| 인증번호 | 숫자 6자리 |
| 코드 유효시간 | 5분 |
| 재발송 간격 | 60초 |
| 코드당 검증 실패 제한 | 5회 |
| 인증 완료 결과 유효시간 | 10분 |
| 세션 저장 키 | `Define.EMAIL_VERIFICATION` |

새 코드 발송 성공 시 이전 코드와 인증 완료 상태가 교체됩니다. 제한은 세션 기준이며, 회원 이메일 중복 검사는 이 기능에 포함되어 있지 않습니다. SMTP 환경 설정이 필요하며 코드·해시를 응답이나 로그에 노출하지 않습니다.

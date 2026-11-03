# 공통 예외 처리 사용법

서비스에서 해당 예외 클래스를 import하고 조건에 따라 던집니다.

```java
import com.busan.eventplatform.global.error.exception.Exception400;

if (amount > 100000) {
    throw new Exception400("최대 충전 금액은 100,000 포인트입니다.");
}
```

| 예외 클래스 | 사용 상황 |
|---|---|
| `Exception400` | 잘못된 요청 |
| `Exception401` | 로그인 필요 |
| `Exception403` | 접근 권한 없음 |
| `Exception404` | 리소스를 찾을 수 없음 |
| `Exception405` | 허용되지 않은 HTTP 메서드 |
| `Exception500` | 서버 처리 실패 |

`GlobalExceptionHandler`가 HTTP 상태를 설정하고 해당 Mustache 오류 화면에 메시지를 전달합니다. 별도의 `try-catch`는 필요하지 않습니다.

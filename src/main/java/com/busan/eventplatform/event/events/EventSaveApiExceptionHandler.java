package com.busan.eventplatform.event.events;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EventSaveApiController.class)
public class EventSaveApiExceptionHandler {

    // 행사 등록 Fetch 요청에만 적용한다.
    // 다른 Controller의 기존 오류 페이지는 변경하지 않는다.

    @ExceptionHandler(Exception400.class)
    public ResponseEntity<Map<String, String>> handle400(Exception400 e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception401.class)
    public ResponseEntity<Map<String, String>> handle401(Exception401 e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(Exception403.class)
    public ResponseEntity<Map<String, String>> handle403(Exception403 e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(Exception404.class)
    public ResponseEntity<Map<String, String>> handle404(Exception404 e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 날짜·숫자 등의 변환 실패를 서버 장애와 구분한다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBinding(BindException e) {
        return error(
                HttpStatus.BAD_REQUEST,
                "날짜·시간·숫자 입력 형식을 확인해주세요."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("행사 등록 요청 처리 실패", e);

        // 내부 예외 내용은 노출하지 않는다.
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "행사 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        );
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }
}
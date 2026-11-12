//package com.busan.eventplatform.global.error.exception;
//
//import com.busan.eventplatform.user.registration.RegistrationApiController;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.util.Map;
//
///**
// * RegistrationApiController 전용 예외 처리.
// * GlobalExceptionHandler 가 에러 "페이지"(mustache)를 반환하더라도
// * Fetch 로 호출하는 신청 API 는 항상 JSON {"message": ...} 를 받도록 보장한다.
// */
//@Slf4j
//@Order(Ordered.HIGHEST_PRECEDENCE)
//@RestControllerAdvice(assignableTypes = RegistrationApiController.class)
//public class RegistrationApiExceptionHandler {
//
//    // 포인트 파트: 이미 결제한 행사
//    @ExceptionHandler(AlreadyPurchaseException.class)
//    public ResponseEntity<Map<String, String>> handleAlreadyPurchase(AlreadyPurchaseException e) {
//        return body(HttpStatus.BAD_REQUEST, e.getMessage());
//    }
//
//    // 정원 초과, 잔액 부족, 중복 신청, 모집 마감 등
//    @ExceptionHandler(Exception400.class)
//    public ResponseEntity<Map<String, String>> handle400(Exception400 e) {
//        return body(HttpStatus.BAD_REQUEST, e.getMessage());
//    }
//
//    // 타인의 신청 취소, 주최자가 아닌 기업의 반려 등
//    @ExceptionHandler(RegistrationForbiddenException.class)
//    public ResponseEntity<Map<String, String>> handleForbidden(RegistrationForbiddenException e) {
//        return body(HttpStatus.FORBIDDEN, e.getMessage());
//    }
//
//    // 회차·신청·유저를 찾을 수 없음
//    @ExceptionHandler(Exception404.class)
//    public ResponseEntity<Map<String, String>> handle404(Exception404 e) {
//        return body(HttpStatus.NOT_FOUND, e.getMessage());
//    }
//
//    // 그 외 예상하지 못한 오류 (원인은 서버 로그에만 남기고, 화면에는 일반 메시지)
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
//        log.error("참가 신청 API 처리 중 예기치 못한 오류", e);
//        return body(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
//    }
//
//    private ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
//        return ResponseEntity.status(status).body(Map.of("message", message));
//    }
//}

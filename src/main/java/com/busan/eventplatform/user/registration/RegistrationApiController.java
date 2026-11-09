package com.busan.eventplatform.user.registration;


import com.busan.eventplatform.login.LoginIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RegistrationApiController {

    private final RegistrationService registrationService;

    // 1. 참가신청 API
    // JSON 요청활용, 상대 경로 지정
    @PostMapping("/api/events/{eventId}/{registrations}")
    public ResponseEntity<?> applyEvent (
            @PathVariable Long eventId,
            // 인증은 서버 세션 기반이며, 세션 키 'loginIdentity' 사용
            @SessionAttribute(name = "loginIdentity", required = false)
            LoginIdentity loginIdentity ) {
        // 로그인 인증 확인 로직 추가
        if(loginIdentity == null) {
            log.warn("참가 신청 실패: 미로그인 사용자의 접근");
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // LoginType.USER(개인 회원) 권한 확인 확인 로직 추가
        if(!loginIdentity.isUser()) {
            log.warn("참가 신청 실패: 권한 없는 자용자(기업/관리자 등)의 접근");
            return ResponseEntity.status(403).body("일반 유저만 참가 신청이 가능합니다.");
        }

        try {
            // Service 로직 호출
            // 참고: 구매 기능은 RegistrationService 호출이나 참가 신청 원장을 생성하지 않음.
            // 구매와 참가 신청의 통합은 별도의 작업
            RegistrationRes res = registrationService.apply(eventId, loginIdentity.getId());
            log.info("참가 신청 성공 - 행사 ID: {}, 유저 ID: {}", eventId, loginIdentity.getId());
            return  ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("참가 신청 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // 참가 신청 취소 API
    @PostMapping("/api/registrations/cancel/{id}")
    public ResponseEntity<?> cancelRegistration(@PathVariable("id") Long registrationId) {
        // 취소 비즈니스 로직 호출 (포인트 환불 등)
        registrationService.cancelRegistration(registrationId);
        return ResponseEntity.ok(Map.of("message", "신청이 성공적으로 취소되었습니다."));
    }



}

package com.busan.eventplatform.user.registration;


import com.busan.eventplatform.login.LoginIdentity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RegistrationApiController {

    private final RegistrationService registrationService;

    // 1. 참가신청 API
    // JSON 요청활용, 상대 경로 지정
    @PostMapping("/api/events/{eventId}/registrations}")
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

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    )
}

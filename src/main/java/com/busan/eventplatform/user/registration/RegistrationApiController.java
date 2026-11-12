package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 참가 신청 API.
 * - 인증: 서버 세션의 LoginSession.IDENTITY 에 저장된 LoginIdentity
 * - 미로그인 401 / 권한 없는 계정 유형 403 은 컨트롤러에서 바로 응답
 * - 비즈니스 예외(정원·잔액·중복 등)는 RegistrationApiExceptionHandler 가 JSON {"message": ...} 로 변환
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RegistrationApiController {

    private final RegistrationService registrationService;

    // 1. 참가 신청 (detail.mustache) — 정원·결제가 회차 단위이므로 sessionId 로 신청
    @PostMapping("/api/event-sessions/{sessionId}/registrations")
    public ResponseEntity<?> apply(
            @PathVariable Long sessionId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            log.warn("참가 신청 실패: 미로그인 사용자 접근 - sessionId={}", sessionId);
            return unauthorized();
        }
        if (!loginIdentity.isUser()) {
            log.warn("참가 신청 실패: 개인 회원이 아닌 계정 - sessionId={}, loginId={}", sessionId, loginIdentity.getId());
            return forbidden("개인 회원만 참가 신청이 가능합니다.");
        }

        RegistrationRes.ApplyDto res = registrationService.apply(sessionId, loginIdentity);
        log.info("참가 신청 성공 - sessionId={}, userId={}, status={}", sessionId, loginIdentity.getId(), res.getStatus());
        return ResponseEntity.ok(res);
    }

    // 2. 내 신청 내역 (mypage.mustache)
    @GetMapping("/api/registrations/me")
    public ResponseEntity<?> myRegistrations(
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            return unauthorized();
        }
        if (!loginIdentity.isUser()) {
            return forbidden("개인 회원만 신청 내역을 조회할 수 있습니다.");
        }
        return ResponseEntity.ok(registrationService.getMyRegistrations(loginIdentity));
    }

    // 3. 신청 취소 (mypage.mustache) — 기존 경로 유지, 본인 확인 추가
    @PostMapping("/api/registrations/cancel/{id}")
    public ResponseEntity<?> cancel(
            @PathVariable("id") Long registrationId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            return unauthorized();
        }
        if (!loginIdentity.isUser()) {
            return forbidden("개인 회원만 신청을 취소할 수 있습니다.");
        }

        registrationService.cancel(registrationId, loginIdentity);
        log.info("신청 취소 성공 - registrationId={}, userId={}", registrationId, loginIdentity.getId());
        return ResponseEntity.ok(Map.of("message", "신청이 성공적으로 취소되었습니다."));
    }

    // 4. 회차별 참가자 목록 (manage.mustache, 주최 기업)
    @GetMapping("/api/event-sessions/{sessionId}/registrations")
    public ResponseEntity<?> participants(
            @PathVariable Long sessionId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            return unauthorized();
        }
        if (!loginIdentity.isCompany()) {
            return forbidden("행사 주최 기업만 조회할 수 있습니다.");
        }
        return ResponseEntity.ok(registrationService.getParticipants(sessionId, loginIdentity));
    }

    // 5. 반려 (manage.mustache, 주최 기업) — 취소 API 와 같은 경로 형식
    @PostMapping("/api/registrations/reject/{id}")
    public ResponseEntity<?> reject(
            @PathVariable("id") Long registrationId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            return unauthorized();
        }
        if (!loginIdentity.isCompany()) {
            return forbidden("행사 주최 기업만 반려할 수 있습니다.");
        }

        registrationService.reject(registrationId, loginIdentity);
        log.info("신청 반려 성공 - registrationId={}, companyId={}", registrationId, loginIdentity.getId());
        return ResponseEntity.ok(Map.of("message", "신청이 반려되었습니다."));
    }

    // ===== 공통 응답 =====

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }

    private ResponseEntity<Map<String, String>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", message));
    }
}
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
 * 참가 신청 API. URL 규칙: /api/registrations/{동작}/{id}
 * - 인증: 서버 세션의 LoginSession.IDENTITY 에 저장된 LoginIdentity
 * - 미로그인 401 / 권한 없는 계정 유형 403 은 컨트롤러에서 바로 응답
 * - 비즈니스 예외(정원·잔액·중복 등)는 RegistrationApiExceptionHandler 가 JSON {"message": ...} 로 변환
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/registrations")
public class RegistrationApiController {

    private final RegistrationService registrationService;
    // 참가 신청은 결제와 함께 처리해야 하므로 흐름 클래스를 통해 호출 (결제 없는 신청 방지)
    private final RegistrationPurchaseService registrationPurchaseService;

    // 1. 신청 가능 여부 + 포인트 확인 (행사 상세: 충전 / 차감 화면 분기용)
    @GetMapping("/check/{sessionId}")
    public ResponseEntity<?> check(
            @PathVariable Long sessionId,
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity loginIdentity) {

        if (loginIdentity == null) {
            return unauthorized();
        }
        if (!loginIdentity.isUser()) {
            return forbidden("개인 회원만 참가 신청이 가능합니다.");
        }
        return ResponseEntity.ok(registrationService.check(sessionId, loginIdentity));
    }

    // 2. 참가 신청 (포인트 차감 + 확정/대기)
    @PostMapping("/apply/{sessionId}")
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

        // 변경 후 — 검증 → 포인트 결제 → 신청 저장 (화면 결제하기와 같은 메서드 → 결제 없는 신청 불가)
        RegistrationRes.ApplyDto res = registrationPurchaseService.applyWithPayment(sessionId, loginIdentity);
        log.info("참가 신청 성공 - sessionId={}, userId={}, status={}", sessionId, loginIdentity.getId(), res.getStatus());
        return ResponseEntity.ok(res);
    }

    // 3. 내 신청 내역
    @GetMapping("/me")
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

    // 4. 신청 취소
    @PostMapping("/cancel/{registrationId}")
    public ResponseEntity<?> cancel(
            @PathVariable Long registrationId,
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

    // 5. 회차별 참가자 목록 (주최 기업)
    @GetMapping("/participants/{sessionId}")
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

    // 6. 반려 (주최 기업)
    @PostMapping("/reject/{registrationId}")
    public ResponseEntity<?> reject(
            @PathVariable Long registrationId,
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
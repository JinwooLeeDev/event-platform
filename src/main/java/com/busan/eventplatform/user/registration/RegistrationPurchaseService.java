package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.login.LoginIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 참가 신청 ↔ 포인트 결제 흐름 담당 (순서 조율 + 트랜잭션 경계만, 비즈니스 규칙은 넣지 않음)
 *
 * - 신청(RegistrationService)과 결제(PurchaseService)가 서로를 직접 부르면 순환 참조로 서버가 시작되지 않음
 *   → 두 서비스는 서로를 모르고, 이 클래스가 양쪽을 순서대로 호출
 * - 검증 → 결제 → 신청 저장을 하나의 트랜잭션으로 묶어, 어느 단계에서 실패해도 전부 롤백
 * - 호출하는 곳: 결제하기(PurchaseController), 참가 신청 API(RegistrationApiController)
 *   → 두 입구가 같은 메서드를 써서 "결제 없는 신청" / "신청 없는 결제"가 생기지 않음
 */
@Service
@RequiredArgsConstructor
public class RegistrationPurchaseService {

    private final RegistrationService registrationService;
    private final PurchaseService purchaseService;

    /**
     * 참가 신청 + 포인트 결제
     * ① prepareApply()   검증만 (개인 회원, 회차 락, 모집 가능, 계정 상태, 중복 신청) — 돈 안 움직임
     * ② pay()            포인트 차감 + 결제 내역 저장 → Purchase 반환
     * ③ completeApply()  확정/대기 + 자리 점유 + 접수 번호 + 신청 저장(결제 연결) + 이력
     */
    @Transactional
    public RegistrationRes.ApplyDto applyWithPayment(Long sessionId, LoginIdentity loginIdentity) {
        // ① 검증 — 실패하면 포인트 차감 시도조차 안 함
        RegistrationService.ApplyContext ctx = registrationService.prepareApply(sessionId, loginIdentity);

        // ② 결제 — 잔액 부족·중복 결제면 예외 → 전체 롤백
        Purchase purchase = purchaseService.pay(ctx.user(), ctx.session());

        // ③ 신청 저장 + 결제 연결
        return registrationService.completeApply(ctx, purchase);
    }

    // TODO(f/6 진행 순서 7·8): 포인트 파트 refund() 완성 후 추가
    //  - cancelWithRefund(): 본인 취소 + 날짜별 차등 환불
    //  - rejectWithRefund(): 주최자 거절 + 전액 환불
}
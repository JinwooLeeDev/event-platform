package com.busan.eventplatform.api.refund;

import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.api.purchase.PurchaseRepository;
import com.busan.eventplatform.api.purchase.PurchaseStatus;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {
    private final PurchaseRepository purchaseRepository;
    private final RefundRepository refundRepository;

    @Transactional
    // User의 환불. --> User 검증 , 환불할 Registration내역 검증받고 들어오므로 User , registration 그대로 사용가능.
    public void refundUser(Long userId , Registration registration , RegistrationStatus beforeStatus) {
        // 1. 전달받은 행사가 이 유저가 실제 결제한 구매행사가 맞는지.
        if(!purchaseRepository.existsEventByUser(userId , registration.getEventSession().getId())) throw new Exception400("해당 행사는 결제한 이력이 없습니다");

        Purchase purchase = purchaseRepository.findById(registration.getPurchase().getId())
                .orElseThrow(() -> new Exception404("해당 행사는 구매한 이력이 없습니다"));

        // 2. 현재 구매된 행사의 등록(Registration)상태가 "취소"이며 이 이벤트가 "진행 예정"이 맞는지.
        if(registration.getStatus() != RegistrationStatus.CANCELLED ||
           registration.getEventSession().getStatus() != EventSessionStatus.SCHEDULED ||
           purchase.getStatus() == PurchaseStatus.CANCELED) {
            throw new Exception400("해당 행사는 환불요청(취소신청)이 불가한 행사입니다");
        }

        // 굳이 또 검사할 필요 없는것같지만 foruserCancel에서 amount가 필요하므로 보안상 검사 한번 더.
        if(purchase.getPrice() != registration.getEventSession().getFee()) {
            throw new Exception400("환불요청한 행사와 구매한 행사의 금액이 일치하지 않습니다");
        }

        // 3. 환불 정책에 맞게 금액 계산.
        int refundAmount = RefundPolicy.forUserCancel(
                purchase.getPrice() , beforeStatus , LocalDate.now() , registration.getEventSession().getStartsAt().toLocalDate());

        // 최종 통과 --> purchase내역의 상태 변경 + refund테이블에 이력 추가 + 유저에게 환불금액 돌려주기.
        Refund refund = Refund.builder()
                .user(registration.getUser())
                .amount(refundAmount)
                .status(RefundStatus.REFUND)
                .purchase(purchase)
                .build();

        registration.getUser().addPoint(refundAmount);
        refundRepository.save(refund);
        purchase.setStatus(PurchaseStatus.CANCELED);

    }


    // Company의 환불.
    public void refundCompany(LoginIdentity loginIdentity, Long eventSessionId) {
    }
}

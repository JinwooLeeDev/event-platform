package com.busan.eventplatform.api.refund;

import com.busan.eventplatform.api.purchase.*;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {
    private final PurchaseRepository purchaseRepository;
    private final RefundRepository refundRepository;
    private final EventSessionRepository eventSessionRepository;
    private final DepositRepository depositRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    /// User 본인 결정으로의 환불. --> User 검증 , 환불할 Registration내역 검증받고 들어오므로 User , registration 그대로 사용가능.
    public void refundUser(Long userId , Registration registration , RegistrationStatus beforeStatus) {
        // 1. 전달받은 행사가 이 유저가 실제 결제한 구매행사가 맞는지.
        if(!purchaseRepository.existsEventByUser(userId , registration.getEventSession().getId() , PurchaseStatus.PURCHASED)){
            throw new Exception400("해당 행사는 결제한 이력이 없습니다");
        }

        Purchase purchase = purchaseRepository.findById(registration.getPurchase().getId())
                .orElseThrow(() -> new Exception404("해당 행사는 구매한 이력이 없습니다"));

        // 2. 현재 구매된 행사의 등록(Registration)상태가 "취소"이며 이 이벤트가 "진행 예정"이 맞는지.
        if(registration.getStatus() != RegistrationStatus.CANCELLED ||
           registration.getEventSession().getStatus() != EventSessionStatus.SCHEDULED ||
           purchase.getStatus() == PurchaseStatus.REFUNDED) {
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
        purchase.setPurchaseRefunded();
    }


    /**
     * 아래 조건을 이미 검사하고 들어오는건지 확인 필요!!!
     * 이 행사가 "진행 후" 또는 "진행중"인 행사는 아닌지? --> 관리자측에서 검사??(보류)
     * 외부에서 필요한 값 : eventSession이 "행사취소" 상태
     */
    @Transactional
    /// Company의 환불 + User 전액 환불(User의 등록상태가 REJECTED 전? 후? --> 언제 환불해줄까?).
    public void refundCompany(Company company , Long eventSessionId , LocalDate cancelReqTime) {
        // 1. 이 행사가 관리자에게 "승인취소" 받았는지
        EventSession eventSession = eventSessionRepository.findByIdForUpdate(eventSessionId)
                .orElseThrow(() -> new Exception400("해당 행사를 찾을 수 없습니다"));
        if(eventSession.isCanceled()) throw new Exception400("승인 취소받은 행사가 아닙니다");


        // 2. 이 기업이 실제 갖고있는 행사가 맞는지 + 이 행사 상태가 "결제완료"가 맞는지 한번 더 검증.
        Deposit deposit = depositRepository.findByCompanyIdAndEventId(company.getId() , eventSession.getEvent().getId())
                .orElseThrow(() -> new Exception400("해당 행사를 개최한 이력이 없습니다"));

        if(deposit.getStatus() != DepositStatus.PAID) throw new Exception400("이 행사는 환불을 진행할 수 없습니다");

        Refund refund = refundRepository.findByDepositId(deposit.getId() , RefundStatus.PENDING)
                .orElseThrow(() -> new Exception404("해당 행사는 결제한 내역이 없습니다 "));

        // 3. 환불 정책에 맞게 환불요금 계산.
        int amount = RefundPolicy.forCompanyCancel(Deposit.SESSION_DEPOSIT , cancelReqTime , LocalDate.from(eventSession.getStartsAt()));

        // 최종 통과. DB반영 + 보증금 환불 + 환불상태 변경.
        company.chargePoint(amount);
        refund.setCompanyRefund(amount);
        deposit.setDepositRefunded();
    }


    /// 기업 취소시 User에게 전액 환불. refundCompany와 함께 사용. --> 같은 트랜잭션에서 검증하고 들어오므로 eventSession 자체로 받아도될듯.
    @Transactional
    public void pointBackUser(EventSession eventSession) {
        List<Registration> registList = registrationRepository.findAllBySessionIdWithUser(eventSession.getId());

        for(Registration r : registList) {
            if(r.getStatus().isActive() && r.getPurchase().getStatus() == PurchaseStatus.PURCHASED &&
                eventSession.getStatus() == EventSessionStatus.CANCELLED) {
                int amount = RefundPolicy.forCompanyReject(r.getPurchase().getPrice()); // user들에게 전액환불금액 계산.
                r.getUser().addPoint(amount); // user들에게 환불금액 돌려줌.
                r.reject(); // 등록상태 변경
                r.getPurchase().setPurchaseRefunded(); // User들의 구매내역은 "환불"로 변경.

                // 유저의 의도는 아니지만 어쨌든 기업으로 인해 환불되었으므로 환불내역에 추가.
                Refund refund = refundRepository.save(
                        Refund.builder()
                                .user(r.getUser())
                                .purchase(r.getPurchase())
                                .status(RefundStatus.REFUND)
                                .amount(amount)
                                .build()
                );
            }
        }
    }


    /**
     * 행사 정상 종료시 기업에게 보증금 자동환급.
     * 외부에서 필요한 값 : eventSession 상태값이 "행사 종료"
     */

    public void refundEventCompleted(Long companyId , Long eventSessionId) {
        // 1. 이 행사를 개최한 기업과 전달받은 기업이 일치하는지.
        EventSession eventSession = eventSessionRepository.findByCompanyIdAndSessionId(companyId , eventSessionId)
                .orElseThrow(() -> new Exception404("해당 행사의 기업을 찾을 수 없습니다"));

        // 2. 행사 정상 종료된 "행사 종료" 상태인지.
        if(eventSession.getStatus() != EventSessionStatus.COMPLETED) throw new Exception400("행사가 정상종료되어야 보증금 환급 가능합니다");

        // 3. 보증금 내역에서 "결제완료" 상태 확인.
        Deposit deposit = depositRepository.findByCompanyIdAndSessionId(companyId, eventSession.getId())
                .orElseThrow(() -> new Exception404("해당 행사의 기업을 찾을 수 없습니다"));
        if(deposit.getStatus() != DepositStatus.PAID) throw new Exception400("결제 완료된 행사가 아닙니다");

        // 최종. 기업에게 보증금 돌려주고 DB반영.
        eventSession.getEvent().getCompany().chargePoint(deposit.getAmount());
        deposit.setDepositReturned();
    }

}

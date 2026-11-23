package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.global.i18n.LocaleMessages;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.refund.RefundRepository;
import com.busan.eventplatform.api.refund.RefundStatus;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.AlreadyPurchaseException;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;

import com.busan.eventplatform.event.eventSession.EventSessionStatus;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventSessionRepository eventSessionRepository;
    private final PaymentService paymentService;
    private final RefundRepository refundRepository;

    // 결제창에 필요한 행사 결제 포인트와 행사 정보 조회.
    public PurchaseRes.PurchasePointDto calcPointAndEvent(Long userId, Long eventSessionId) {
        EventSession eventSession = eventSessionRepository.findByIdWithEvent(eventSessionId)
                .orElseThrow(() -> new Exception404("결제하려는 행사가 존재하지 않습니다"));

            User user = userRepository.findById(userId).orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

            return PurchaseRes.PurchasePointDto.builder()
                    .eventSession(eventSession)
                    .currentPoint(user.getPoint())
                    .build();
    }

    // [참가 신청 흐름 전용] 검증이 끝난 회원·회차로 결제만 수행 (RegistrationPurchaseService ② 단계)
    // 신청 트랜잭션에 합류 → 이후 신청 저장 실패 시 함께 롤백
    @Transactional
    public Purchase pay(User user, EventSession eventSession) {
        // 1. 같은 회차 중복 결제 확인.
        if (purchaseRepository.existsEventByUser(user.getId(), eventSession.getId() , PurchaseStatus.PURCHASED)) {
            throw new AlreadyPurchaseException("이미 결제처리된 행사입니다");
        }

        // 2. 포인트 차감 (잔액 부족 시 예외)
        user.deductPoint(eventSession.getFee());

        // 3. 결제 내역 저장 후 반환 (신청에 purchase_id로 연결)
        Purchase purchase = Purchase.builder()
                .user(user)
                .eventSession(eventSession)
                .price(eventSession.getFee())
                .build();
        return purchaseRepository.save(purchase);
    }


    public List<PurchaseRes.PurchaseListDto> purchaseList(Long userId) {
        List<Purchase> purchaseList = purchaseRepository.findByUserIdWithPurchaseList(userId);
        if (purchaseList == null || purchaseList.isEmpty()) return List.of();

        // registrationList 조회할때 동일 userId로 조회하므로 이벤트세션ID만 필요. --> map으로
        List<Long> sessionIds =purchaseList.stream()
                .map(p -> p.getEventSession().getId())
                .distinct() // 동일 회차 등록 제거.
                .toList();

        List<Registration> registrationList =
                registrationRepository.findAllByUserIdAndSessionIds(userId , sessionIds);

        Map<Long, Registration> registrationMap = new HashMap<>();

        for(Registration r : registrationList) {
            Long eventSessionId = r.getEventSession().getId();
            Registration savedRegistration = registrationMap.get(eventSessionId);

            if(savedRegistration == null || r.getId() > savedRegistration.getId()) {
                registrationMap.put(r.getEventSession().getId() , r);
            }
        }

        return purchaseList.stream()
                .map(p ->
                        new PurchaseRes.PurchaseListDto(p , registrationMap.get(p.getEventSession().getId())))
                .toList();
    }

    public List<PurchaseRes.PointHistoryDto> pointHistory(Long userId) {
        var refundList = refundRepository.findByUserIdWithPurchase(userId, RefundStatus.REFUND);
        Map<Long, Integer> refundedAmounts = new HashMap<>();
        refundList.forEach(refund -> refundedAmounts.merge(refund.getPurchase().getId(), refund.getAmount(), Integer::sum));
        var history = new ArrayList<PurchaseRes.PointHistoryDto>();

        // ★ 기업 거절로 생긴 환불 구분 — 환불된 결제 중 신청이 '거절(REJECTED)'인 결제 id (쿼리 1번)
        List<Long> refundedPurchaseIds = refundList.stream().map(refund -> refund.getPurchase().getId()).distinct().toList();
        Set<Long> rejectedPurchaseIds = refundedPurchaseIds.isEmpty() ? Set.of()
                : new HashSet<>(registrationRepository.findPurchaseIdsByStatus(refundedPurchaseIds, RegistrationStatus.REJECTED));


        paymentService.paymentList(userId).forEach(payment -> history.add(PurchaseRes.PointHistoryDto.builder()
                .occurredAt(payment.getPaidAt()).typeLabel(LocaleMessages.display("충전")).title(LocaleMessages.display("포인트 충전"))
                .statusLabel("충전 완료").amount(payment.getAmount()).build()));

        purchaseList(userId).forEach(purchase -> history.add(PurchaseRes.PointHistoryDto.builder()
                .occurredAt(purchase.getPurchaseDate()).typeLabel(LocaleMessages.display("행사 구매")).title(purchase.getTitle())
                .sessionName(purchase.getSessionName()).detailUrl(purchase.getDetailUrl())
                .statusLabel(refundLabel(refundedAmounts.get(purchase.getPurchaseId()), purchase.getPrice()))
                .amount(purchase.getPrice()).debit(true).build()));

        refundList.forEach(refund -> {
            var purchase = refund.getPurchase();
            var session = purchase.getEventSession();

            // ★ 기업 거절 = 신청이 거절됐고 회차·행사는 취소되지 않음 (행사 취소도 신청 상태는 REJECTED라 제외)
            boolean rejectedByCompany = rejectedPurchaseIds.contains(purchase.getId())
                    && session.getStatus() != EventSessionStatus.CANCELLED
                    && session.getEvent().getStatus() != EventStatus.CANCELLED;

            history.add(PurchaseRes.PointHistoryDto.builder()
                    .occurredAt(refund.getCreatedAt()).typeLabel(LocaleMessages.display(rejectedByCompany ? "기업 거절" : "환불"))   // ★ 변경
                    .title(session.getEvent().getTitle())
                    .sessionName(session.getName()).detailUrl("/event/" + session.getEvent().getId())
                    .statusLabel(refundLabel(refund.getAmount(), purchase.getPrice()))
                    .amount(refund.getAmount()).build());
        });

        history.sort(Comparator.comparing(PurchaseRes.PointHistoryDto::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return history;
    }

    private String refundLabel(Integer amount, int purchasePrice) {
        if (amount == null) return LocaleMessages.display("구매 완료");
        if (amount == 0) return LocaleMessages.display("환불액 0P");
        return LocaleMessages.display(amount >= purchasePrice ? "전액 환불" : "부분 환불");
    }
}

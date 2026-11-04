package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;

    // 처음 결제요청시 결제건 생성.
    @Transactional
    public String createPayment(Long userId, int amount) {
    String paymentId = createPaymentId(userId);

    // TODO userId로 user 조회. userRepository 통해서.  추후 변경.
    User user = paymentRepository.findUser(userId).orElseThrow(() -> new RuntimeException("유저없음 추후 변경."));

    Payment payment = Payment.builder()
            .user(user)
            .amount(amount)
            .paymentId(paymentId)
            .build();
    paymentRepository.save(payment);

        return paymentId;
    }

    private String createPaymentId(Long userId) {
        return "payment-" + userId + "_" + UUID.randomUUID().toString().substring(0 , 5);
    }

    @Transactional
    public PaymentRes.CompleteDto complete(Long userId, PaymentRes.PortOnePayment portOnePayment , String paymentId) {
        // TODO userId로 user 조회. userRepository 통해서.  추후 변경.
        User user = paymentRepository.findUser(userId).orElseThrow(() -> new RuntimeException("유저없음 추후 변경."));

        // 1. 우리 서버가 요청한 결제가 맞는지 확인.
        Payment payment = paymentRepository.findByPayment(paymentId)
                .orElseThrow(() -> new RuntimeException("결제건이 맞지 않음"));

        // 2. 현재 결제가 현재유저의 결제가 맞는지.
        if(!payment.isOwner(user.getId())) throw new RuntimeException("동일 유저 아님");

        // 3. 중복지급 방지(이미 "PAID"라면 거절.)
        if(payment.isPaid()) throw new RuntimeException("이미 결제완료된 요청");

        // 4. 포트원 기준 결제 상태 확인.
        if(!"PAID".equals(portOnePayment.getStatus())) throw new RuntimeException("포트원 서버 문제");

        // 5. 금액 위변조 확인.
        if(portOnePayment.getAmount() == null ||
           portOnePayment.getAmount().getTotal() == null ||
           portOnePayment.getAmount().getTotal() != payment.getAmount()) {
            throw new RuntimeException("요청하신 금액과 일치하지 않습니다. 다시 시도해주세요");
        }

        // 6. 최종 DB 반영 + status 값 변경.
        payment.confirmPay();
        log.info("payment 수정 후 : {}" , payment.getStatus());
        return new PaymentRes.CompleteDto(payment.getAmount());
    }

}

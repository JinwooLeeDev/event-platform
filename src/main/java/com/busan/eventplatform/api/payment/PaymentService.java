package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.UserRepository;
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
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    // user 가져오기.
    public User sessionUser(Long identityId) {
        return userRepository.findById(identityId).orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다."));
    }


    // 처음 결제요청시 결제건 생성.
    @Transactional
    public String createPayment(LoginIdentity loginIdentity, int amount) {
        Payment payment;
        String paymentId = createPaymentId(loginIdentity.getId());

        if(loginIdentity.isUser()){
            User user = userRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다."));

            payment = Payment.builder()
                    .user(user)
                    .amount(amount)
                    .paymentId(paymentId)
                    .build();
        }
        else if (loginIdentity.isCompany()) {
            Company company = companyRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));

            payment = Payment.builder()
                    .company(company)
                    .amount(amount)
                    .paymentId(paymentId)
                    .build();
        }
        else {
            throw new Exception403("로그인 유형(User 또는 Company)이 올바르지 않습니다");
        }

        paymentRepository.save(payment);

            return paymentId;
    }

    private String createPaymentId(Long identityId) {
        return "payment-" + identityId + "_" + UUID.randomUUID().toString().substring(0 , 5);
    }

    @Transactional
    public PaymentRes.CompleteDto complete(LoginIdentity loginIdentity , PaymentRes.PortOnePayment portOnePayment , String paymentId) {
        // 1. 우리 서버가 요청한 결제가 맞는지 확인.
        Payment payment = paymentRepository.findByPayment(paymentId)
                .orElseThrow(() -> new Exception404("존재하지 않는 결제건입니다."));

        // 2. 현재 결제가 현재 세션이용자의 결제가 맞는지.
        if(!payment.isOwner(loginIdentity)) throw new Exception400("");

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

        // 6. 최종 DB 반영 + status 값 변경 + 유저 / company point 변경.(더티체킹)
        payment.confirmPay();

        if(loginIdentity.isUser()) payment.getUser().chargePoint(payment.getAmount());
        else if(loginIdentity.isCompany()) payment.getCompany().chargePoint(payment.getAmount());
        else throw new Exception400("결제 처리가 완료되지 않았습니다.");

        return new PaymentRes.CompleteDto(payment.getAmount());
    }

    // 포인트 충전 , 결제 , 환불시 화면에 즉시 보여지기 위해 현재 보유 포인트 가져오는 메서드.
    public int currentPoint(LoginIdentity loginIdentity) {
        if(loginIdentity.isUser()) {
            User user = userRepository.findById(loginIdentity.getId())
                    .orElseThrow(() -> new Exception404("유저를 찾을 수 없음"));
            return user.getPoint();
        }
        else if(loginIdentity.isCompany()) {
                Company company = companyRepository.findById(loginIdentity.getId())
                        .orElseThrow(() -> new Exception404("유저를 찾을 수 없음"));
                return company.getPoint();
        } else throw new Exception400("로그인 유형(User | Company)이 올바르지 않습니다");
    }

}

package com.busan.eventplatform;

import com.busan.eventplatform.api.payment.Payment;
import com.busan.eventplatform.api.payment.PaymentRepository;
import com.busan.eventplatform.api.payment.PaymentStatus;
import com.busan.eventplatform.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("h2")
public class PaymentTest {

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private TestEntityManager em;

        @Test
        @DisplayName("paymentId로 결제 내역을 조회한다")
        void findByPayment_test() {

            // given
            // data.sql에 User ID 1번이 존재한다고 가정
            User user = em.find(User.class, 1L);
            assertThat(user).isNotNull();

            String paymentId = "payment-test-123";

            Payment payment = Payment.builder()
                    .user(user)
                    .amount(10000)
                    .paymentId(paymentId)
                    .build();

            em.persistAndFlush(payment);
            em.clear();

            // when
            Payment result = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow();

            // then
            assertThat(result.getPaymentId()).isEqualTo(paymentId);
            assertThat(result.getAmount()).isEqualTo(10000);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(result.getUser().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 paymentId를 조회하면 Optional.empty가 반환된다")
        void findByPayment_notFound_test() {

            // when
            Optional<Payment> result =
                    paymentRepository.findByPaymentId("not-exist-payment");

            // then
            assertThat(result).isEmpty();
        }
    }


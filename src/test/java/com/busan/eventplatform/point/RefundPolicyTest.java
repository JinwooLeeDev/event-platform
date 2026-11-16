package com.busan.eventplatform.point;

import com.busan.eventplatform.api.refund.RefundPolicy;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundPolicyTest {

    private final int paidAmount = 6000;
    private final LocalDate today = LocalDate.now();

    @Test
    void 무료행사는_환불금액이_0원() {
        int result = RefundPolicy.forUserCancel(
                0,
                RegistrationStatus.CONFIRMED,
                today,
                today.plusDays(5)
        );

        assertEquals(0, result);
    }

    @Test
    void 대기열에서_취소하면_전액환불() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.WAITING,
                today,
                today.plusDays(1)
        );

        assertEquals(6000, result);
    }

    @Test
    void 행사_4일이상_전이면_전액환불() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.CONFIRMED,
                today,
                today.plusDays(4)
        );

        assertEquals(6000, result);
    }

    @Test
    void 행사_3일전이면_3분의2_환불() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.CONFIRMED,
                today,
                today.plusDays(3)
        );

        assertEquals(4000, result);
    }

    @Test
    void 행사_2일전이면_절반환불() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.CONFIRMED,
                today,
                today.plusDays(2)
        );

        assertEquals(3000, result);
    }

    @Test
    void 행사_1일전이면_환불금액이_0원() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.CONFIRMED,
                today,
                today.plusDays(1)
        );

        assertEquals(0, result);
    }

    @Test
    void 행사_당일이면_환불금액이_0원() {
        int result = RefundPolicy.forUserCancel(
                paidAmount,
                RegistrationStatus.CONFIRMED,
                today,
                today
        );

        assertEquals(0, result);
    }
}

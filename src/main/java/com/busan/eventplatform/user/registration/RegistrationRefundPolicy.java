package com.busan.eventplatform.user.registration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 참가 신청 환불 금액 계산 규칙 (조장님 확정 D5) — 계산만 하고 포인트는 움직이지 않는다.
 * 실제 포인트 반환은 포인트 파트 PurchaseService.refund() 가 담당한다.
 *
 * [개인 회원 본인 취소] — 회차 시작 날짜 기준
 *   4일 이상 전: 전액 / 3일 전: 2/3 / 2일 전: 1/2 / 1일 전·당일: 0 (취소는 가능)
 *   회차 시작 후: 취소 불가 (RegistrationService.getCancelBlockedReason 에서 차단)
 *   대기(WAITING) 상태 취소: 좌석을 받지 못했으므로 날짜와 상관없이 전액
 * [주최자 거절] 전액
 *
 * "N일 전"은 시각이 아닌 날짜 기준 (11/21 행사를 11/18에 취소 = 3일 전), 소수점은 내림.
 */
public final class RegistrationRefundPolicy {

    private RegistrationRefundPolicy() {}  // 계산 전용 — 객체 생성 불필요

    /**
     * 개인 회원 본인 취소 시 환불 금액
     *
     * @param paidAmount         실제 결제 금액 (Purchase.price)
     * @param statusBeforeCancel 취소 직전 상태 (CONFIRMED / WAITING)
     * @param cancelDate         취소한 날짜
     * @param sessionDate        회차 시작 날짜
     */
    public static int forUserCancel(int paidAmount, RegistrationStatus statusBeforeCancel,
                                    LocalDate cancelDate, LocalDate sessionDate) {
        if (paidAmount <= 0) {
            return 0;  // 무료 행사(0P 결제)는 환불할 금액 없음
        }
        if (statusBeforeCancel == RegistrationStatus.WAITING) {
            return paidAmount;  // 대기 취소: 전액 환불
        }

        long daysBefore = ChronoUnit.DAYS.between(cancelDate, sessionDate);

        if (daysBefore >= 4) {
            return paidAmount;            // 4일 이상 전: 전액
        }
        if (daysBefore == 3) {
            return paidAmount * 2 / 3;    // 3일 전: 2/3 (정수 나눗셈 = 내림)
        }
        if (daysBefore == 2) {
            return paidAmount / 2;        // 2일 전: 1/2
        }
        return 0;                         // 1일 전·당일: 환불 불가
    }

    /** 주최 기업의 참가 거절 시 환불 금액 — 전액 */
    public static int forCompanyReject(int paidAmount) {
        return Math.max(paidAmount, 0);
    }
}
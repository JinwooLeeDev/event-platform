package com.busan.eventplatform.company;

import com.busan.eventplatform.api.payment.Payment;
import com.busan.eventplatform.api.payment.PaymentStatus;
import com.busan.eventplatform.api.payment.PaymentTypeStatus;
import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.api.refund.Refund;
import com.busan.eventplatform.api.refund.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CompanyPointResponse {

    @Getter
    @AllArgsConstructor
    public static class Detail {
        private String pointText;
        private List<Charge> charges;
        private List<DepositHistory> deposits;
        private List<RefundHistory> refunds;

        public int getChargeCount(){return charges.size();}
        public int getDepositCount(){return deposits.size();}
        public int getRefundCount(){return refunds.size();}
    }

    @Getter
    public static class Charge {
        private String amountText;
        private String statusName;
        private String paymentType;
        private String paidAtText;
        private PaymentTypeStatus typeStatus;

        public Charge(Payment payment) {
            this.amountText = formatAmount(payment.getAmount());
            this.statusName = payment.getStatus().getDisplayName();
            this.paymentType = payment.getTypeStatus().name();
            this.paidAtText = formatDate(payment.getPaidAt());
            this.typeStatus = payment.getTypeStatus();
        }

    }

    // 회차 보증금 내역 한 건
    @Getter
    public static class DepositHistory {
        private Long eventId;
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String amountText;
        private String statusName;
        private String paidAtText;
        private String returnedAmountText;

        public DepositHistory(Deposit deposit) {
            this.eventId = deposit.getEventSession().getEvent().getId();
            this.sessionId = deposit.getEventSession().getId();
            this.eventTitle = deposit.getEventSession().getEvent().getTitle();
            this.sessionName = deposit.getEventSession().getName();

            // 원래 보증금액. 취소 환급액과는 구분
            this.amountText = formatAmount(deposit.getAmount());
            this.statusName = deposit.getStatus().getDisplayName();

            // READY의 생성 시각을 실제 납부 시각으로 표시하지 않음
            this.paidAtText = deposit.getStatus() == DepositStatus.READY
                    ? "-"
                    : formatDate(deposit.getPaidAt());

            // 정상 반환 완료인 경우에만 전액 반환 금액 표시
            this.returnedAmountText =
                    deposit.getStatus() == DepositStatus.RETURN
                            ? formatAmount(deposit.getAmount())
                            : "-";
        }
    }

    // 취소 환급 내역 한 건
    @Getter
    public static class RefundHistory {
        private Long eventId;
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String amountText;
        private String statusName;
        private String reason;
        private String requestedAtText;

        public RefundHistory(Refund refund) {
            Deposit deposit = refund.getDeposit();

            this.eventId = deposit.getEventSession().getEvent().getId();
            this.sessionId = deposit.getEventSession().getId();
            this.eventTitle = deposit.getEventSession().getEvent().getTitle();
            this.sessionName = deposit.getEventSession().getName();

            // 대기·거절 상태의 금액을 실제 환급액으로 표시하지 않음
            this.amountText = refund.getStatus() == RefundStatus.REFUND
                    ? formatAmount(refund.getAmount())
                    : "-";

            // 현재 RefundStatus의 getter는 getDisplayname()임
            this.statusName = refund.getStatus().getDisplayname();
            this.reason = refund.getReason() == null
                    ? "-"
                    : refund.getReason();

            // createdAt은 요청 기록 생성 시각이며 환급 완료 시각이 아님
            this.requestedAtText = formatDate(refund.getCreatedAt());
        }
    }

    // 금액을 화면 표시용 문자열로 변환
    private static String formatAmount(int amount) {
        return String.format(Locale.KOREA, "%,d", amount);
    }

    // 저장된 시각을 화면 표시용 문자열로 변환
    private static String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }

        return timestamp.toLocalDateTime().format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    }
}

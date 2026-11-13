package com.busan.eventplatform.api.refund;

public enum RefundStatus {
    READY("환불 대기"), // 환불 신청 날짜 확인
    PENDING("관리자 승인 대기") , // 기업의 행사 취소 신청.
    REFUND("환불 완료") ; // 환불 구분.


    private final String displayname;
    private RefundStatus(String displayname) {this.displayname = displayname;}

    public String getDisplayname() {
        return displayname;
    }
}

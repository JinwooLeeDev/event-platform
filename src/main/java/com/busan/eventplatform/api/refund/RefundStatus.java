package com.busan.eventplatform.api.refund;

public enum RefundStatus {
    READY("환불 대기"),
    PENDING("관리자 승인 대기") ,
    REFUND("환불 완료") ;


    private final String displayname;
    private RefundStatus(String displayname) {this.displayname = displayname;}

    public String getDisplayname() {
        return displayname;
    }
}

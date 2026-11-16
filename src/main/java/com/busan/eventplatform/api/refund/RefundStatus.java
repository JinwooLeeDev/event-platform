package com.busan.eventplatform.api.refund;

public enum RefundStatus {
    REJECTED("환불 거절") , //TODO 기업이 환불요청시 Refund 생성하므로 관리자측에서 "환불거절"시 상태변경 필요.
    PENDING("관리자 승인 대기") , // 기업의 행사 취소 신청시 초기상태.
    REFUND("환불 완료") ; // 환불 구분(User , 기업 공통).


    private final String displayname;
    private RefundStatus(String displayname) {this.displayname = displayname;}

    public String getDisplayname() {
        return displayname;
    }
}

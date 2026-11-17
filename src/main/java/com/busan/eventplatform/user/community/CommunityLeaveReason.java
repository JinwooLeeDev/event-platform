package com.busan.eventplatform.user.community;

import com.busan.eventplatform.user.registration.RegistrationStatus;

/**
 * 클래스의 역할
 * - 퇴장 사유(참가 취소/주최자 거절/커뮤니티 종료/행사 회차 취소)
 * - 멤버십 행(community_members)에 기록만 남긴다 (화면 안내 문구·통계용).
 */
public enum CommunityLeaveReason {
    CANCELLED("참가 취소"),             // 확정자가 본인 취소
    REJECTED("주최자 거절"),            // 기업이 거절 (진입 경로 차단 — 활성 멤버가 아니므로 자동으로 막힘)
    CLOSED("커뮤니티 종료"),            // 회차 종료일 24시 (5단계 스케줄러에서 사용)
    EVENT_CANCELLED("행사 회차 취소");   // ★ [f/41] 운영자가 회차 삭제를 승인 → 방 전체 종료·삭제

    private final String description;

    CommunityLeaveReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** 신청 상태 변경 결과 → 퇴장 사유 (거절이면 REJECTED, 그 외 확정 이탈은 CANCELLED) */
    public static CommunityLeaveReason from(RegistrationStatus after) {
        return after == RegistrationStatus.REJECTED ? REJECTED : CANCELLED;
    }

}

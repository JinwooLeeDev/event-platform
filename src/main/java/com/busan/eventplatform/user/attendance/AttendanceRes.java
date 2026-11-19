package com.busan.eventplatform.user.attendance;

import lombok.Builder;
import lombok.Getter;

/**
 * ★ [f/42] 출석 화면용 DTO (머스태치용 — 숨길 값은 null)
 */
public class AttendanceRes {

    /** [기업] 출석 QR 화면 */
    @Getter
    @Builder
    public static class QrPageDto {
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String scheduleText;     // 2026. 11. 27 (금) · 오후 1:09
        private String checkPath;        // /attendance/check/{회차 id}?code={서명} — 화면 JS가 사이트 주소를 붙여 QR 생성
        private String checkTimeText;    // 출석 가능 시간 안내
        private long attendedCount;      // 출석 인원
        private long confirmedCount;     // 참가 확정 인원
    }

    /** [참가자] QR 인식 후 출석 확인·결과 화면 */
    @Getter
    @Builder
    public static class CheckPageDto {
        private Long sessionId;
        private String code;             // POST 로 다시 보낼 QR 서명 (유효할 때만)
        private String eventTitle;       // 회차를 찾지 못하면 null
        private String sessionName;
        private String scheduleText;
        private String checkTimeText;
        private boolean loginRequired;   // 로그인 안내 + 로그인 버튼
        private boolean canSubmit;       // [출석 체크] 버튼 표시
        private boolean done;            // 출석 완료 (이번에 했거나 이미 했음)
        private String message;          // 안내·오류 문구
        private String messageClass;     // is-info / is-success / is-error
    }
}
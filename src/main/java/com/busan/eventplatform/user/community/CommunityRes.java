package com.busan.eventplatform.user.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * [f/40] 커뮤니티 화면 데이터 (머스태치용)
 * - 머스태치(JMustache)는 boolean의 isXxx() 를 못 읽는 경우가 있어 getXxx() 를 직접 둔다 (f/6 교훈)
 * - 화면에서 섹션 밖 {{값}} 으로 쓰는 문자열은 null 이면 안 됨 → null 일 수 있는 값은 {{#값}}{{.}}{{/값}} 로만 출력 (5-2 교훈)
 */
public class CommunityRes {

    /** 채팅방 화면 /community/{sessionId} */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomDto {
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String closeText;              // "2026. 11. 23 24시에 커뮤니티가 종료됩니다"
        private String endDayNotice;           // [5-2] 종료일 11시 이후 안내 문구, 아니면 null
        private String myKey;                  // ★ [6단계] "user:36002" / "company:5" — 내 메시지 판단
        private boolean host;                  // ★ [6단계] 보는 사람이 주최 기업인지 (공지 입력창 표시)
        private String hostName;               // ★ [6단계] 주최 기업명
        private String notice;                 // ★ [6단계] 상단 고정 공지, 없으면 null
        private String noticeUpdatedAt;        // ★ [6단계] 공지 수정 시각, 없으면 null
        private List<ParticipantDto> participants; // ★ [6단계] 참여자 목록 (활성 멤버)
        private int participantCount;          // ★ [6단계]
        private List<ChatItemDto> messages;    // 오래된 것 → 최신 순
        private boolean empty;

        public boolean getEmpty() {
            return empty;
        }

        public boolean getHost() {
            return host;
        }
    }

    /** 채팅 한 줄 (또는 입장·퇴장 기록 한 줄) */
    @Getter
    @AllArgsConstructor
    public static class ChatItemDto {
        private String nickname;  // 기업이면 "기업명 (주최)" / 입장·퇴장 기록이면 null (화면에서 쓰지 않음)
        private String content;
        private String sentAt;    // "11.16 17:05"
        private boolean mine;     // 내가 쓴 메시지면 오른쪽 정렬
        private boolean host;     // ★ [6단계] 주최 기업 메시지면 색 구분
        private boolean system;   // ★ [6단계] 입장·퇴장 기록이면 가운데 회색 한 줄

        public boolean getMine() {
            return mine;
        }

        public boolean getHost() {
            return host;
        }

        public boolean getSystem() {
            return system;
        }
    }

    /** ★ [6단계] 참여자 한 명 */
    @Getter
    @AllArgsConstructor
    public static class ParticipantDto {
        private Long userId;      // 실시간 퇴장 시 목록에서 지우는 기준
        private String nickname;
    }

    /** 내 커뮤니티 목록 /community */
    @Getter
    @AllArgsConstructor
    public static class MyCommunityDto {
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String closeText;  // "2026. 11. 23 24시까지"
        private String url;        // "/community/36002"
    }
}

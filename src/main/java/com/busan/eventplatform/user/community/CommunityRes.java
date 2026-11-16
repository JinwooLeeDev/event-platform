package com.busan.eventplatform.user.community;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * [f/40] 커뮤니티 화면 데이터 (머스태치용)
 * - 머스태치(JMustache)는 boolean의 isXxx() 를 못 읽는 경우가 있어 getXxx() 를 직접 둔다 (f/6 교훈)
 */
public class CommunityRes {

    /** 채팅방 화면 /community/{sessionId} */
    @Getter
    @AllArgsConstructor
    public static class RoomDto {
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String closeText;           // "2026. 11. 23 24시에 커뮤니티가 종료됩니다"
        private Long myUserId;
        private List<ChatItemDto> messages; // 오래된 것 → 최신 순
        private boolean empty;

        public boolean getEmpty() {
            return empty;
        }
    }

    /** 채팅 한 줄 */
    @Getter
    @AllArgsConstructor
    public static class ChatItemDto {
        private String nickname;
        private String content;
        private String sentAt;   // "11.16 17:05"
        private boolean mine;    // 내가 쓴 메시지면 오른쪽 정렬

        public boolean getMine() {
            return mine;
        }
    }

    /** 내 커뮤니티 목록 /community */
    @Getter
    @AllArgsConstructor
    public static class MyCommunityDto {
        private Long sessionId;
        private String eventTitle;
        private String sessionName;
        private String closeText;  // "11. 23 24시까지"
        private String url;        // "/community/36002"
    }
}

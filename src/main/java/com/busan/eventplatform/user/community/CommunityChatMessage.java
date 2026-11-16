package com.busan.eventplatform.user.community;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [f/40] 커뮤니티 채팅 메시지 (Q8: DB 저장 + 입장 시 최근 50개)
 * - 퇴장해도 남긴다 (Q3) / 커뮤니티 종료 시 삭제 (5단계, Q11-1)
 * - 입장·퇴장 같은 시스템 알림은 저장하지 않고 실시간으로만 보낸다
 */
@Entity
@Table(name = "community_messages",
        indexes = @Index(name = "idx_community_message_session_id", columnList = "event_session_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private CommunityChatMessage(EventSession eventSession, User user, String content, LocalDateTime createdAt) {
        this.eventSession = eventSession;
        this.user = user;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static CommunityChatMessage create(EventSession eventSession, User user, String content) {
        return new CommunityChatMessage(eventSession, user, content, LocalDateTime.now());
    }

}

package com.busan.eventplatform.user.community;


import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [f/40] 커뮤니티 멤버십 — 회차 하나 = 커뮤니티 방 하나 (Q2)
 * - 참가 확정자만 활성 멤버 (left_at IS NULL)
 * - 퇴장해도 행을 지우지 않고 left_at·left_reason만 기록 → 재신청 후 다시 확정되면 같은 행을 다시 활성화
 * - (event_session_id, user_id) 유니크 → 한 회원은 한 회차 방에 행 1개
 */
@Entity
@Table(name = "community_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_community_member_session_user",
                columnNames = {"event_session_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 마지막으로 입장한 시각 (재입장하면 갱신)
     */
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 퇴장 시각 — null이면 활성 멤버
     */
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CommunityLeaveReason leftReason;

    private CommunityMember(EventSession eventSession, User user, LocalDateTime joinedAt) {
        this.eventSession = eventSession;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    /**
     * 처음 입장
     */
    public static CommunityMember join(EventSession eventSession, User user, LocalDateTime now) {
        return new CommunityMember(eventSession, user, now);
    }

    public boolean isActive() {
        return leftAt == null;
    }

    /**
     * 다시 입장 (취소·거절 후 재신청해서 다시 확정된 경우)
     */
    public void rejoin(LocalDateTime now) {
        this.joinedAt = now;
        this.leftAt = null;
        this.leftReason = null;
    }

    /**
     * 퇴장 — 이미 퇴장한 멤버면 아무것도 하지 않음 (중복 이벤트에도 안전)
     */
    public void leave(CommunityLeaveReason reason, LocalDateTime now) {
        if (!isActive()) {
            return;
        }
        this.leftAt = now;
        this.leftReason = reason;
    }

}

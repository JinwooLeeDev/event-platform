package com.busan.eventplatform.user.community;

import com.busan.eventplatform.company.Company;
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
 * - ★ [6단계] 작성자는 참가 회원(user) 또는 주최 기업(company) 중 하나 — 나머지 하나는 null
 * - ★ [6단계] 입장·퇴장 기록도 저장 (kind = JOIN / LEAVE, user = 입장·퇴장한 회원)
 *            → 새로고침·늦게 들어온 사람도 채팅 사이 원래 위치에서 볼 수 있음
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
    @JoinColumn(name = "user_id")            // ★ [6단계] nullable — 기업이 쓴 메시지면 null
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")         // ★ [6단계] 주최 기업이 쓴 메시지
    private Company company;

    /** ★ [6단계] 메시지 종류 — CHAT(채팅) / JOIN·LEAVE(입장·퇴장 기록) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Kind kind;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Kind { CHAT, JOIN, LEAVE }

    private CommunityChatMessage(EventSession eventSession, User user, Company company, Kind kind,
                                 String content, LocalDateTime createdAt) {
        this.eventSession = eventSession;
        this.user = user;
        this.company = company;
        this.kind = kind;
        this.content = content;
        this.createdAt = createdAt;
    }

    /** 참가 회원의 메시지 */
    public static CommunityChatMessage create(EventSession eventSession, User user, String content) {
        return new CommunityChatMessage(eventSession, user, null, Kind.CHAT, content, CommunityPolicy.now());
    }

    /** ★ [6단계] 주최 기업의 메시지 */
    public static CommunityChatMessage createByCompany(EventSession eventSession, Company company, String content) {
        return new CommunityChatMessage(eventSession, null, company, Kind.CHAT, content, CommunityPolicy.now());
    }

    /** ★ [6단계] 입장·퇴장 기록 — user = 입장·퇴장한 회원, content = "○○님이 입장했습니다." */
    public static CommunityChatMessage memberEvent(EventSession eventSession, User user, Kind kind, String content) {
        return new CommunityChatMessage(eventSession, user, null, kind, content, CommunityPolicy.now());
    }

    /** 입장·퇴장 기록인지 (화면에서 가운데 회색 한 줄로 표시) */
    public boolean isSystem() {
        return kind != Kind.CHAT;
    }

    public boolean isByCompany() {
        return company != null;
    }

    /** 화면에 보여줄 작성자 이름 — 기업은 "기업명 (주최)" */
    public String getWriterName() {
        return isByCompany() ? company.getName() + " (주최)" : user.getDisplayName();
    }

    /** 이 메시지를 쓴 사람이 viewer 인지 (내 메시지 → 오른쪽 정렬) */
    public boolean isWrittenBy(CommunityViewer viewer) {
        if (isSystem()) {
            return false;
        }
        if (viewer.isCompany()) {
            return company != null && company.getId().equals(viewer.id());
        }
        return user != null && user.getId().equals(viewer.id());
    }
}
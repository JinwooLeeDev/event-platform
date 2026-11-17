package com.busan.eventplatform.user.community;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [f/40 6단계] 커뮤니티 상단 고정 공지 (Q15)
 * - 회차(방)당 1개 — 주최 기업만 등록·수정, 새 공지가 이전 공지를 대체
 * - 커뮤니티 종료 시 메시지와 함께 삭제 (5단계 closeExpired)
 */
@Entity
@Table(name = "community_notices",
        uniqueConstraints = @UniqueConstraint(name = "uk_community_notice_session", columnNames = "event_session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    /** 마지막으로 공지를 쓴 기업 (주최 기업) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private CommunityNotice(EventSession eventSession, Company company, String content, LocalDateTime now) {
        this.eventSession = eventSession;
        this.company = company;
        this.content = content;
        this.updatedAt = now;
    }

    public static CommunityNotice create(EventSession eventSession, Company company, String content, LocalDateTime now) {
        return new CommunityNotice(eventSession, company, content, now);
    }

    /** 공지 수정 — 이전 내용을 대체 */
    public void edit(Company company, String content, LocalDateTime now) {
        this.company = company;
        this.content = content;
        this.updatedAt = now;
    }
}

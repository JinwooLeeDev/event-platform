package com.busan.eventplatform.user;

import com.busan.eventplatform.event.EventSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 참가 신청 원장

@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [추가] 행사 회차 연관 관계 매핑 (지연 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EventSession session; // todo - 추후 윤아씨 구현 예정인 점 확인 완료

    // 신청자 연관관계 매핑(지연 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    // 대기 번호 (CONFIRMED 등은 null, WAITING 상태인 경우에만 값 할당)
    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Registration(EventSession session, User user, RegistrationStatus status, Integer queueNumber) {
        this.session = session;
        this.user = user;
        this.status = status;
        this.queueNumber = queueNumber;
    }


    // 엔티티 영속화 시 시간 자동 기록
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // 엔티티 수정 시 시간 자동 기록
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
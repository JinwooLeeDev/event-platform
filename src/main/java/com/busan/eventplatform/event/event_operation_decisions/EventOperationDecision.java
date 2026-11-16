package com.busan.eventplatform.event.event_operation_decisions;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_operation_decisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOperationDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EventSession eventSession;

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    // 확정 이니원 스냅샷
    @Column(nullable = false)
    private int confirmedCountSnapshot;

    // 최소 인원 스냅샷
    @Column(nullable = false)
    private int minimumCountSnapshot;

    // 결정 유형 (승인, 취소)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventOperationState state;

    // 결정 사유
    @Column(length = 1000)
    private String reason;

    // 생성 일시
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static EventOperationDecision create(
            Event event,
            EventSession eventSession,
            User actor,
            int confirmedCountSnapshot,
            int minimumCountSnapshot,
            EventOperationState state
    ) {
        EventOperationDecision decision = new EventOperationDecision();

        decision.event = event;
        decision.eventSession = eventSession;
        decision.actor = actor;
        decision.confirmedCountSnapshot = confirmedCountSnapshot;
        decision.minimumCountSnapshot = minimumCountSnapshot;
        decision.state = state;

        return decision;
    }
}

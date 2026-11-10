package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "event_favorites",
        uniqueConstraints = {@UniqueConstraint(name = "uk_event_favorite_user_event",
        columnNames = {"user_id", "event_id"})})
public class EventFavorite {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 좋아요 누른 시간 -- 좋아요 목록에서 정렬할 때 사용
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public EventFavorite(User user, Event event) {
        this.user = user;
        this.event = event;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}

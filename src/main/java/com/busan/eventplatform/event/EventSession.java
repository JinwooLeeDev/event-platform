package com.busan.eventplatform.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 윤아님과 공유 완료된 참가비 필드 (기존 participation_fee_point)
    @Column(name = "fee")
    private Integer fee;

    // [추가] 행사 최대 정원
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    // 행사(Event)와의 다대일(N:1) 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // 테스트 시 임시 더미 데이터 생성을 위한 Builder
    @Builder
    public EventSession(Integer fee, Integer capacity, Event event) {
        this.fee = fee;
        this.capacity = capacity;
        this.event = event;
    }
}
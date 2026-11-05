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

    // 테스트 시 임시 더미 데이터 생성을 위한 Builder
    @Builder
    public EventSession(Integer fee) {
        this.fee = fee;
    }
}
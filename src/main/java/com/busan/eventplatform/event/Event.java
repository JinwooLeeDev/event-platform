package com.busan.eventplatform.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_tb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 행사 제목
    @Column(nullable = false)
    private String title;

    // 행사 상태
    @Column(nullable = false)
    private String status;

    // 행사 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 행사 시작 시간
    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    // 행사 종료 시간
    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    // 행사 취소 시간
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
}

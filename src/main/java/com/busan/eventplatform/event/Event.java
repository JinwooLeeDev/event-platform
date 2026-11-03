package com.busan.eventplatform.event;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "event_tb")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status; // 확정 승인대기중 , 개최 확정 , 취소.

    @Column(nullable = false)
    private String description;

    private Timestamp startsAt;
    private Timestamp endsAt;
    private Timestamp canceledAt;
}

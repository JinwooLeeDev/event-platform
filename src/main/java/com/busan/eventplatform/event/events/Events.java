package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategory;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "event_tb")
public class Events {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    // 주최 기업
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "company_id", nullable = false)
//    private Company companyId;

    // 행사 카테고리
    @ManyToOne(fetch = FetchType.LAZY) // 카테고리 하나를 여러 게시물이 사용 가능
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    // 행사 제목
    @Column(nullable = false)
    private String title;

    // 확정 승인대기중, 개최 확정, 취소
    @Column(nullable = false)
    private String status;

    // 행사 설명
    @Column(nullable = false)
    private String description;

    // 모집 시작일
    @Column(nullable = false)
    private Timestamp startsAt;

    // 모집 마감일
    private Timestamp endsAt;

    // 취소된 날짜와 시간 저장
    private Timestamp canceledAt;

    private String cancellation_reason;
}

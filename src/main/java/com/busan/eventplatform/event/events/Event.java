package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주최 기업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 행사 카테고리
    @ManyToOne(fetch = FetchType.LAZY) // 카테고리 하나를 여러 게시물이 사용 가능
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    // 행사 주소
    @Column(nullable = false)
    private String address;

    // 행사 제목
    @Column(nullable = false)
    private String title;

    // 목록 형태로 보여줄 짧은 요약
    @Column(nullable = false)
    private String summary;

    // 행사 등록 및 진행 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // 행사 설명
    @Column(nullable = false)
    private String description;

    // 모집 시작일
    @Column(nullable = false)
    private LocalDateTime applyStartAt;

    // 모집 마감일
    @Column(nullable = false)
    private LocalDateTime applyEndAt;

    public boolean isApplicationClosed() {
        return applyEndAt != null && !LocalDateTime.now().isBefore(applyEndAt);
    }

    // 행사 정보가 공개된 시각 -- 목록 정렬 유용
    @Column
    private LocalDateTime publishedAt; // 공개 전에는 null

    // 취소된 날짜와 시간 저장
    private LocalDateTime cancelledAt; // 취소 전에는 null

    // 취소 사유
    private String cancellationReason; // 취소 전에는 null

    // 관리자 보완 요청 내용
    @Column(length = 1000)
    private String feedbackMessage;

    // 행사 정보가 생성된 시각 -- 미승인 초안 정리, 확인
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성 시각 넣기
    @PrePersist // 처음 저장하기 직전에 실행 -- 생성 시 자동 채움
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwner(Long companyId) {
        return this.company.getId().equals(companyId);
    }

    public boolean isPaidStatus() {return this.getStatus() == EventStatus.PAID_READY;}
    public boolean isPendingStatus() {return this.getStatus() == EventStatus.PENDING_REVIEW;}

}

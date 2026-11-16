package com.busan.eventplatform.company.companyAdminReview;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "company_review_history")
public class CompanyReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    심사 대상 기업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

//    심사를 처리할 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

//    심사 전 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private CompanyStatus fromStatus;

//    심사 후 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private CompanyStatus toStatus;

//    관리자 사유
    @Column(columnDefinition = "TEXT")
    private String feedback;

//    이력 생성시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Builder
    public CompanyReviewHistory(
            Company company,
            User reviewer,
            CompanyStatus fromStatus,
            CompanyStatus toStatus,
            String feedback) {
        this.company = company;
        this.reviewer = reviewer;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.feedback = feedback;
    }


}

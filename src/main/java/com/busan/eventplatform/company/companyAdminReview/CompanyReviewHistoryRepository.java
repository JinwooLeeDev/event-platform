package com.busan.eventplatform.company.companyAdminReview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyReviewHistoryRepository extends JpaRepository<CompanyReviewHistory, Long> {

    @Query("""
            SELECT crh
            from CompanyReviewHistory crh
            JOIN FETCH crh.company
            WHERE crh.company.id = :companyId
            ORDER BY crh.createdAt DESC, crh.id DESC
            """)
    List<CompanyReviewHistory> findByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId);
}

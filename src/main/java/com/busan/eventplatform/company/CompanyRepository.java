package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c WHERE c.email = :email")
    Optional<Company> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Company c WHERE c.businessNumber = :businessNumber")
    Optional<Company> findByBusinessNumber(@Param("businessNumber") String businessNumber);

    @Query("""
            SELECT c 
            FROM Company c
            WHERE c.status = :status
            AND c.businessVerificationStatus IN :verificationStatuses
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Company> findReviewTargets(@Param("status")CompanyStatus status,
                                    @Param("verificationStatuses")List<BusinessVerificationStatus> verificationStatuses);

}

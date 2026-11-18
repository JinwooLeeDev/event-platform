package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 포인트 충전결제 요청을 동시에 두번 이상 시도할 경우 가장 마지막에 반영된 포인트충전 금액만 충전될 경우를 대비하여 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> LockFindById(@Param("id") Long id);

//    이메일 중복 여부만 확인할 때 사용
    boolean existsByEmail(String email);
}

package com.busan.eventplatform.api.refund;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [임시 생성]
 * 포인트 파트 담당자가 본 파일(RefundRepository)을 공식적으로 구현하기 전까지,
 * Branch 5의 환불 내역 저장(save) 테스트를 위해 임시로 사용하는 레포지토리입니다.
 */

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    @Query("""
        select r from Refund r where r.deposit.id = :depositId and r.status = :status
""")
    Optional<Refund> findByDepositId(@Param("depositId") Long depositId , @Param("status") RefundStatus status);
}

package com.busan.eventplatform.api.refund;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [임시 생성]
 * 포인트 파트 담당자가 본 파일(RefundRepository)을 공식적으로 구현하기 전까지,
 * Branch 5의 환불 내역 저장(save) 테스트를 위해 임시로 사용하는 레포지토리입니다.
 */

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

}

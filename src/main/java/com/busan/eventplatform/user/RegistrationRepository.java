package com.busan.eventplatform.user;

import com.busan.eventplatform.event.EventSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    // 유저의 특정 회차 중복 신청 여부 검증용
    boolean existsBySessionIdAndUserId(Long sessionId , Long userId);

    // [동시성 제어] 세션 정보를 비관적 락으로 조회하여 정원 동시 접근 제어
    // 특정 행사에 동시에 몰리는 것을 줄세우기(직렬화)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EventSession s where s.id = :sessionId")
    EventSession findSessionWithPessimisticLock(@Param("sessionId") Long sessionId);

    // 현재 확정 (또는 승인 대기)된 인원 수 조회
    long countBySessionIdAndStatusIn(Long sessionId, List<RegistrationStatus> statuses);

    // 가장 마지막 대기 번호 조회(WATING 상태인 경우)
    @Query("select max(r.queueNumber) from Registration r where r.session.id = :sessionId and r.status = :status")
    Integer findMaxQueueNumberBySessionId(@Param("sessionId") Long sessionId, @Param("status") RegistrationStatus status);

    // 빈자리 발생 시 승격 대상자 조회
    List<Registration> findBySessionIdAndStatusOrderByQueueNumberAsc(Long sessionId, RegistrationStatus status, Pageable pageable);
}

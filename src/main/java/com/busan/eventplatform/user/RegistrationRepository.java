package com.busan.eventplatform.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    // 유저의 특정 회차 중복 신청 여부 검증용
    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}

package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.user.attendance.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationHistoryRepository extends JpaRepository<Attendance, Long> {

    // 추후 특정 신청의 변경 이력 조회 등을 위한 쿼리 메서드 추가 가능
}

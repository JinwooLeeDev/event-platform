package com.busan.eventplatform.user.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 추후 출석 여부 조회 등을 위한 쿼리 메서드 추가


}

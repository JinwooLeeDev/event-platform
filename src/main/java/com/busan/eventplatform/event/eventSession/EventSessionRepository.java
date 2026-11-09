package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [임시 생성]
 * 행사(Event) 파트 담당자가 본 파일(EventSessionRepository)을 공식적으로 구현하기 전까지,
 * Branch 5의 참가 신청(apply) 로직에서 행사 정보를 조회(findById)하기 위해 임시로 사용하는 레포지토리입니다.
 */

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {
}

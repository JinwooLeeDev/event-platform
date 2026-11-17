package com.busan.eventplatform.user.community;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [f/40] 서버 시작 시 확정자 → 커뮤니티 멤버십 맞추기
 * - 시드 데이터(data.sql)의 확정 신청은 이벤트 없이 들어오므로 멤버십이 없음 → 시작할 때 채워 넣음
 * - 리스너가 실패해 어긋난 멤버십도 재시작하면 다시 맞춰짐
 * - join() 은 이미 활성 멤버면 아무것도 안 하므로 여러 번 실행해도 안전
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityMembershipInitializer {
    private final RegistrationRepository registrationRepository;
    private final CommunityService communityService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncConfirmedMembers() {
        List<Registration> confirmed = registrationRepository.findAllByStatus(RegistrationStatus.CONFIRMED);
        // getEventSession().getId() / getUser().getId() 는 프록시의 id만 읽으므로 추가 조회가 없음
        confirmed.forEach(r -> communityService.join(r.getEventSession().getId(), r.getUser().getId()));
        log.info("[community] 시작 시 확정자 멤버십 동기화 {}건", confirmed.size());
    }

}

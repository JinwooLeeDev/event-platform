package com.busan.eventplatform.event.event_operation_decisions;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventOperationDecisionService {

    private final EventOperationDecisionRepository decisionRepository;
    private final EventSessionRepository eventSessionRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public void decide(Long sessionId, User actor) {

        EventSession session = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("회차를 찾을 수 없습니다."));

        if (session.getStatus() == EventSessionStatus.CANCELLED) {
            throw new Exception400("이미 취소된 회차입니다.");
        }

        // 현재 확정 인원 조회
        int confirmedCount = (int) registrationRepository.countConfirmedBySession(sessionId);

        // 최소 진행 인원 조회
        int minimumCount = session.getMinimumParticipants();

        // 최소 인원 충족 여부에 따라 결정
        EventOperationState state =
                confirmedCount >= minimumCount
                ? EventOperationState.APPROVED : EventOperationState.CANCELLED;

        // 운영 취소라면 회차 상태 변경
        if (state == EventOperationState.CANCELLED) {
            session.cancel();
        }

        // 운영 결정 이력 저장
        EventOperationDecision decision = EventOperationDecision.create(
                session.getEvent(),
                session,
                actor,
                confirmedCount,
                minimumCount,
                state
        );

        decisionRepository.save(decision);
    }

}

package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventSessionService {

    private final EventSessionRepository eventSessionRepository;
    private final EventRepository eventRepository;
    private final RegistrationService registrationService;

    // 회차 등록
    @Transactional
    public EventSession save(Long eventId, Long companyId, EventSessionRequest.SaveDto saveDto) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("해당 행사을 찾을 수 없습니다."));
        if (!event.getCompany().getId().equals(companyId)) {
            throw new Exception403("해당 행사에 회차를 등록할 권한이 없습니다.");
        }
        saveDto.validate();
        EventSession eventSession = saveDto.toEntity(event);

        return eventSessionRepository.save(eventSession);
    }

    // 회차 단건 조회
    public EventSessionResponse.DetailDto findById(Long id) {
        EventSession eventSession = eventSessionRepository.findById(id)
                .orElseThrow(() -> new Exception404("해당 회차를 찾을 수 없습니다."));

        return new EventSessionResponse.DetailDto(eventSession);
    }

    // 행사별 회차 목록 조회
    public List<EventSession> findByEventId(Long eventId) {
        return eventSessionRepository.findByEventId(eventId);
    }

    // 행사별 상태별 회차 조회
    public List<EventSession> findAllByEventIdAndStatus(Long eventId, EventSessionStatus status) {
        return eventSessionRepository.findAllByEventIdAndStatus(eventId, status);
    }

    // 회차 수정
    @Transactional
    public EventSession update(Long id, EventSessionRequest.UpdateDto updateDto, Long companyId) {
        EventSession eventSession = eventSessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new Exception404("해당 회차를 찾을 수 없습니다."));
        Event event = eventSession.getEvent();
        if (!event.getCompany().getId().equals(companyId)) {
            throw new Exception403("해당 행사에 회차를 수정할 권한이 없습니다.");
        }
        // 수정 전 정원 저장
        int previousCapacity = eventSession.getCapacity();

        eventSession.update(updateDto);

        // 정원이 증가했다면 대기자 승격
        if (eventSession.getCapacity() > previousCapacity) {
            registrationService.promoteWaitingUsers(id);
        }
        return eventSession;
    }

    // 회차 삭제
    @Transactional
    public void delete(Long id, Long companyId) {
        EventSession eventSession = eventSessionRepository.findById(id)
                .orElseThrow(() -> new Exception404("해당 회차를 찾을 수 없습니다."));
        Event event = eventSession.getEvent();
        if (!event.getCompany().getId().equals(companyId)) {
            throw new Exception403("해당 행사에 회차를 삭제할 권한이 없습니다.");
        }
        eventSessionRepository.delete(eventSession);
    }

}

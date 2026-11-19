package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        saveDto.validate();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("해당 행사를 찾을 수 없습니다."));
        if (!event.getCompany().getId().equals(companyId)) {
            throw new Exception403("해당 행사에 회차를 등록할 권한이 없습니다.");
        }
        // 날짜 검증
        EventSession.validateDates(
                saveDto.getStartsAt(),
                saveDto.getEndsAt(),
                saveDto.getApplyStartAt(),
                saveDto.getApplyEndAt()
        );
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
    public List<EventSessionResponse.DetailDto> findDetailsByEventId(Long eventId) {
        return eventSessionRepository.findByEventId(eventId).stream().map(EventSessionResponse.DetailDto::new).toList();
    }

    public List<EventSessionResponse.EventSessionDto> findByEventId(Long eventId) {
        return eventSessionRepository.findByEventId(eventId)
                .stream()
                .map(session -> {
                    String closedReason = registrationService.getClosedReason(session);
                    return new EventSessionResponse.EventSessionDto(session, closedReason);
                })
                .toList();
    }

    // 행사별 상태별 회차 조회 — ★ [F02] 일반 목록(findByEventId)과 같은 화면용 DTO로 반환 (capacityLabel·신청 가능 여부 포함)
    public List<EventSessionResponse.EventSessionDto> findAllByEventIdAndStatus(Long eventId, EventSessionStatus status) {
        return eventSessionRepository.findAllByEventIdAndStatus(eventId, status)
                .stream()
                .map(session -> new EventSessionResponse.EventSessionDto(session, registrationService.getClosedReason(session)))
                .toList();
    }

    // 회차 수정
    @Transactional
    public EventSession update(Long id, EventSessionRequest.UpdateDto updateDto, Long companyId) {
        updateDto.validate();
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

        // 신청 이력이 없는 경우 실제 삭제
        int deletedCount = eventSessionRepository.deleteIfNoRegistrationHistory(id);

        // 신청 이력이 있으면 취소 처리
        if (deletedCount == 0) {
            eventSession.setStatus(EventSessionStatus.CANCELLED);
        }
    }
}

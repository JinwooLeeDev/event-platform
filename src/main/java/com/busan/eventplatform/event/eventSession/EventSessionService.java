package com.busan.eventplatform.event.eventSession;

import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.community.CommunityPolicy;
import com.busan.eventplatform.user.registration.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventSessionService {
    private final CompanyRepository companyRepository;
    private final EventSessionRepository eventSessionRepository;
    private final EventRepository eventRepository;
    private final RegistrationService registrationService;
    private final RefundService refundService;

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

    // 행사별 상태별 회차 조회
    public List<EventSession> findAllByEventIdAndStatus(Long eventId, EventSessionStatus status) {
        return eventSessionRepository.findAllByEventIdAndStatus(eventId, status);
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


    /// 현재 정상 종료된 이벤트의 세션 id를 찾음.(event가 확정된 이벤트 + 세션이 진행중인 세션 + 보증금이 결제된 ) -- 편의메서드
    public List<Long> completedSessionIds(LocalDateTime now){
        return eventSessionRepository.findBySessionIdsAsCompleted(now , EventStatus.PUBLISHED , EventSessionStatus.IN_PROGRESS , DepositStatus.PAID);
    }

    /// 행사 정상 종료시 그 세션의 상태 변경과 기업에게 자동환불.
    @Transactional
    public void completedSession(Long sessionId , LocalDateTime now){
        EventSession es = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("현재 행사를 찾을 수 없습니다"));

        if(es.getEndsAt().isAfter(now) || es.getStatus() != EventSessionStatus.IN_PROGRESS || es.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new Exception404("정상 종료된 행사가 없습니다");
        }

        Company company = companyRepository.LockFindById(es.getEvent().getCompany().getId())
                .orElseThrow(() -> new Exception404("해당 기업은 행사를 개최한 기업이 아닙니다 "));

        es.setCompleted();
        refundService.refundEventCompleted(company.getId() , es.getId());
    }


    @Transactional
    public void startSession(Long sessionId) {
        EventSession es = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("회차를 찾을 수 없습니다"));
        LocalDateTime now = CommunityPolicy.now();

        if(es.getStatus() != EventSessionStatus.SCHEDULED || es.getStartsAt().isAfter(now) || !es.getEndsAt().isAfter(now)) {
            return;
        }

        es.setInProgress();
    }
}
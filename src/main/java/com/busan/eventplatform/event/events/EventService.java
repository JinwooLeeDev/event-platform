package com.busan.eventplatform.event.events;

import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.DepositRepository;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final CompanyRepository companyRepository;
    private final RegistrationRepository registrationRepository;
    private final EventSessionService eventSessionService;
    private final EventSessionRepository eventSessionRepository; /// 행사 결제할때 필요한 행사 회차수 조회를 위해 추가.
    private final com.busan.eventplatform.event.event_file.EventFileService eventFileService;
    private final DepositRepository depositRepository;

    // 행사 등록 기능
    @Transactional
    public Event save(EventRequest.SaveDto saveDto, Long companyId, MultipartFile file) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));

        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new Exception403("승인된 기업만 신청할 수 있습니다.");
        }

        EventCategory category = eventCategoryRepository.findById(saveDto.getCategoryId())
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));

        Event event = saveDto.toEntity();
        event.setCompany(company);
        event.setCategory(category);
        event.setStatus(EventStatus.PENDING_REVIEW);
        eventRepository.save(event);

        if (saveDto.isHasSessions()) {
            for (EventSessionRequest.SaveDto sessionDto : saveDto.getSessions()) {

                eventSessionService.save(event.getId(), companyId, sessionDto);
            }
        } else {
            // 회차가 없는 행사: 기본 회차 자동 생성
            EventSessionRequest.SaveDto sessionDto = new EventSessionRequest.SaveDto();
            sessionDto.setName("기본 회차");
            sessionDto.setStartsAt(saveDto.getStartDate().atStartOfDay());
            sessionDto.setEndsAt(saveDto.getEndDate().atTime(23, 59, 59));
            sessionDto.setCapacity(Integer.MAX_VALUE);
            // 신청 인원 제한 -- 무제한
            sessionDto.setHoldDurationSeconds(0);
            // 좌석 임시 점유 시간 0초 설정
            sessionDto.setPricingType(saveDto.getPricingType());
            sessionDto.setFee(saveDto.getFee());
            // 행사 유료/무료 여부와 참가비 기본 회차에도 적용
            sessionDto.setReservationDepositPoints(0);
            sessionDto.setMinimumParticipants(1);
            sessionDto.setMinimumCheckAt(sessionDto.getEndsAt());

            sessionDto.setApplyStartAt(saveDto.getApplyStartAt());
            sessionDto.setApplyEndAt(saveDto.getApplyEndAt());

            eventSessionService.save(event.getId(), companyId, sessionDto);
        }
        eventFileService.attachFile(event.getId(), file);

        return event;
    }

    // 행사 목록 조회 기능
    public EventResponse.PageDto findAllPage(int page, int size, String keyword) {
        return findAllPage(page, size, keyword, null);
    }

    public EventResponse.PageDto findAllPage(int page, int size, String keyword, Long categoryId) {

        // 페이지 번호 0부터 시작하도록 반환
        int pageIndex = Math.max(0, page - 1);
        // 페이지 크기 1 ~ 50
        int validSize = Math.max(1, Math.min(50, size));
        // 최신 행사 먼저 오도록 id 내림차순 정렬
        Pageable pageable = PageRequest.of(pageIndex, validSize, Sort.by(Sort.Direction.DESC, "id"));
        // 행사 조회
        Page<Event> eventPage;
        eventPage = eventRepository.findPublishedFiltered(EventStatus.PUBLISHED, categoryId,
                keyword == null ? "" : keyword.trim(), pageable);

        var response = new EventResponse.PageDto(eventPage);
        populateSessionPeriods(response.getEventList());
        return response;
    }

    // 행사 상세 조회 기능
    @Transactional
    public EventResponse.DetailDto findById(Long id, Long companyId, Long userId) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        boolean owner = companyId != null && event.getCompany().getId().equals(companyId);

        // 확정 + 승인 대기 인원
        Long confirmedCount = registrationRepository.countTotalByEventIdAndStatus(id, RegistrationStatus.CONFIRMED)
                + registrationRepository.countTotalByEventIdAndStatus(id, RegistrationStatus.PENDING);
        // 대기 인원
        Long waitingCount = registrationRepository.countTotalByEventIdAndStatus(id, RegistrationStatus.WAITING);

        // 참가 확정 회원인지 확인
        boolean chatAvailable = false;

        if (userId != null) {
            chatAvailable = registrationRepository.countConfirmedByEventIdAndUserId(id, userId, RegistrationStatus.CONFIRMED) > 0;
        }
        return new EventResponse.DetailDto(event, owner, confirmedCount, waitingCount, chatAvailable);
    }

    // 행사 수정 화면 데이터 조회
    @Transactional(readOnly = true)
    public EventResponse.UpdateFormDto update(Long id, Long companyId) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("행사 수정 권한이 없습니다.");
        }
        if (EventStatus.PUBLISHED.equals(event.getStatus())) {
            throw new Exception400("승인된 행사는 수정할 수 없습니다.");
        }
        if (!EventStatus.FEEDBACK_REQUIRED.equals(event.getStatus())) {
            throw new Exception400("보완 요청된 행사만 수정할 수 있습니다.");
        }
        return new EventResponse.UpdateFormDto(event);
    }

    // 수정 내용 저장
    @Transactional
    public Event update(Long id, Long companyId, EventRequest.UpdateDto updateDto) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("행사 수정 권한이 없습니다.");
        }
        if (EventStatus.PUBLISHED.equals(event.getStatus())) {
            throw new Exception400("승인된 행사는 수정할 수 없습니다.");
        }
        if (!EventStatus.FEEDBACK_REQUIRED.equals(event.getStatus())) {
            throw new Exception400("보완 요청된 행사만 수정할 수 있습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(updateDto.getCategoryId())
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));
        // 수정된 행사 내용 저장
        updateDto.toEntity(event, category);

        return event;
    }

    // 보완 요청 - 수정 후 재제출 (수정된 내용 저장, 상태 변경, 메시지 초기화)
    @Transactional
    public Event updateAndResubmit(Long id, Long companyId, EventRequest.UpdateDto updateDto) {
        return updateAndResubmit(id, companyId, updateDto, null);
    }

    @Transactional
    public Event updateAndResubmit(Long id, Long companyId, EventRequest.UpdateDto updateDto, org.springframework.web.multipart.MultipartFile file) {
        updateDto.validate();
        if (updateDto.getSessions() != null) {
            var ids = new java.util.HashSet<Long>();
            for (var session : updateDto.getSessions()) {
                if (session == null || session.getId() == null || !ids.add(session.getId())) throw new Exception400("회차 정보를 확인해주세요.");
                session.validate();
                if (!id.equals(eventSessionService.findById(session.getId()).getEventId())) throw new Exception403("다른 행사의 회차는 수정할 수 없습니다.");
            }
        }
        update(id, companyId, updateDto);
        if (updateDto.getSessions() != null) for (var session : updateDto.getSessions()) eventSessionService.update(session.getId(), session, companyId);
        Event event = resubmit(id, companyId);
        if (file != null && !file.isEmpty()) eventFileService.attachFile(id, file);
        return event;
    }

    @Transactional
    public Event resubmit(Long id, Long companyId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));

        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("재제출 권한이 없습니다.");
        }

        if (!EventStatus.FEEDBACK_REQUIRED.equals(event.getStatus())) {
            throw new Exception400("보완 요청된 행사만 재제출할 수 있습니다.");
        }

        event.setStatus(EventStatus.PENDING_REVIEW);
        event.setFeedbackMessage(null);

        return event;
    }

//    // 행사 삭제 기능
//    @Transactional
//    public void delete(Long id, Long companyId) {
//        Event event = eventRepository.findById(id)
//                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
//        if (companyId == null || !event.isOwner(companyId)) {
//            throw new Exception403("삭제 권한이 없습니다.");
//        }
//        if (registrationRepository.existsByEventSessionEventId(id)) {
//            throw new Exception400("신청자가 있는 행사는 삭제할 수 없습니다.");
//        }
//
//        eventRepository.delete(event);
//    }
//
//    // 행사 취소 기능
//    @Transactional
//    public Event cancel(Long id, EventRequest.CancelDto cancelDto) {
//        Event event = eventRepository.findById(id)
//                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
//        if (EventStatus.CANCELLED.equals(event.getStatus())) {
//            throw new Exception400("이미 취소된 행사입니다.");
//        }
//        event.setStatus(EventStatus.CANCELLED);
//        event.setCancelledAt(LocalDateTime.now());
//        event.setCancellationReason(cancelDto.getCancellationReason());
//
//        return event;
//    }

    // 행사 승인 기능 (관리자)
    @Transactional
    public Event approve(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
            throw new Exception400("승인 대기 중인 행사만 승인할 수 있습니다.");
        }
        event.setStatus(EventStatus.PAID_READY); // 관리자가 승인했다면 '결제대기'로 변경.

        return event;
    }

    // 보완 요청 (관리자 -> 기업)
    @Transactional
    public Event requestFeedback(Long id, String feedbackMessage) {
        if (feedbackMessage == null || feedbackMessage.isBlank() || feedbackMessage.trim().length() > 1000) {
            throw new Exception400("보완 요청 사유를 1~1,000자로 입력해주세요.");
        }
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
            throw new Exception400("승인 대기 중인 행사만 보완 요청할 수 있습니다.");
        }
        event.setStatus(EventStatus.FEEDBACK_REQUIRED);
        event.setFeedbackMessage(feedbackMessage.trim());

        return event;
    }

    // 기업이 보완 요청받은 행사 목록 조회
    public List<Event> findAllFeedback(Long companyId) {
        return eventRepository.findAllByCompanyIdAndStatus(
                companyId,
                EventStatus.FEEDBACK_REQUIRED
        );
    }

    // 기업이 등록한 행사 전체 내역 조회
    /// 각 행사마다 갖고있는 회차 수 추가.
    public EventResponse.ListDto findAllByCompanyId(Long companyId) {
        List<Event> eventList = eventRepository.findAllByCompanyId(companyId);
        var response = new EventResponse.ListDto(eventList);
        populateSessionPeriods(response.getEventList(), companyId);
        return response;
    }

    // 일반 목록: 기존 회차 일정 정보만 구성
    private void populateSessionPeriods(
            List<EventResponse.EventDto> events
    ) {
        populateSessionPeriods(events, null); // 기업 보증금 조회는 하지 않는다
    }

    // 기업 행사 목록에서 호출: 회차 일정과 보증금 정보를 함께 구성
    private void populateSessionPeriods(List<EventResponse.EventDto> events, Long companyId) {
        if (events.isEmpty()) return; // 행사가 없으면 조회할 회차도 없으므로 종료

//        화면에 필요한 회차를 한 번에 조회
        List<Long> eventIds = events.stream().map(EventResponse.EventDto::getId).toList();

        List<EventSession> sessions = eventSessionRepository.findAllByEventIds(eventIds);
        Map<Long, Deposit> depositBySessionId = new HashMap<>();
        boolean companyApproved = false;

        // 기업 목록에서만 보증금 정보 조회
        if (companyId != null && !sessions.isEmpty()) {
            Company company = companyRepository.findById(companyId).orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));

            companyApproved = company.getStatus() == CompanyStatus.APPROVED;

            // 해당 행사들에 속한 회차를 한 번에 조회
            List<Long> sessionIds = sessions.stream().map(EventSession::getId).toList();
            // 현재 기업의 해당 회차 보증금 기록을 한 번에 조회
            List<Deposit> deposits = depositRepository.findAllByCompanyIdAndSessionIds(companyId, sessionIds);

            // [회차 ID → Deposit] 형태로 보관
            for (Deposit deposit : deposits) {
                depositBySessionId.put(deposit.getEventSession().getId(), deposit);
            }
        }

        // 완성한 회차 DTO들을 행사별로 묶기 위한 저장 공간
        Map<Long, List<EventResponse.SessionPeriodDto>> byEvent = new HashMap<>();

        // 화면에 표시할 납부 시각 형식
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

        for (EventSession session : sessions) {
            // 기존 생성자로 회차 이름·모집 기간·ID 설정
            EventResponse.SessionPeriodDto dto = new EventResponse.SessionPeriodDto(session);

            // 기업 목록에만 보증금 표시 정보를 추가
            if (companyId != null) {
                // 현재 회차의 보증금 기록 조회. 기록이 없으면 null
                Deposit deposit = depositBySessionId.get(session.getId());

                // 기록이 있으면 실제 금액, 없으면 기본 보증금 표시
                dto.setDepositAmountText(
                        String.format(
                                Locale.KOREA,
                                "%,d",
                                deposit == null ? Deposit.SESSION_DEPOSIT : deposit.getAmount()
                        )
                );

                // 기록이 없으면 미납, 있으면 기록의 상태명 표시
                dto.setDepositStatusName(
                        deposit == null ? "미납" : deposit.getStatus().getDisplayName()
                );

                // 기록에 납부 시각이 있으면 화면용 문자열로 변환
                if (deposit != null && deposit.getPaidAt() != null) {
                    dto.setDepositPaidAt(
                            deposit.getPaidAt().toLocalDateTime().format(format)
                    );
                }

                // 현재 결제 Service는 기존 Deposit을 재사용하지 않으므로
                // 납부 기록이 없는 회차만 신규 결제 허용
                dto.setDepositPayable(
                        companyApproved && session.forSessionDeposit() && deposit == null
                );
            }

            // 해당 행사의 회차 목록에 DTO 추가
            // 목록이 아직 없으면 새로 생성
            byEvent.computeIfAbsent(
                    session.getEvent().getId(),
                    key -> new ArrayList<>()
            ).add(dto);
        }

        for (EventResponse.EventDto event : events) {
            // 해당 행사에 속한 회차 DTO 목록 조회. 없으면 빈 목록 사용
            List<EventResponse.SessionPeriodDto> periods = byEvent.getOrDefault(event.getId(), List.of());

            // 행사 DTO에 회차 목록과 회차 수 설정
            event.setSessionPeriods(periods);
            event.setSessionCount(periods.size());
        }
    }

    public void validateRegistrationPermission(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));

        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new Exception403("기업 승인 완료 후 행사를 등록할 수 있습니다.");
        }
    }
}

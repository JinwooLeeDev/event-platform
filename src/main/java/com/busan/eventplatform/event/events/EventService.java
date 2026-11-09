package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final CompanyRepository companyRepository;

    // 행사 등록 기능
    @Transactional
    public Event save(EventRequest.SaveDto saveDto, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));
        EventCategory category = eventCategoryRepository.findById(saveDto.getCategoryId())
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));

        Event event = saveDto.toEntity();
        event.setCompany(company);
        event.setCategory(category);
        event.setStatus(EventStatus.PENDING_REVIEW);

        return eventRepository.save(event);
    }

    // 행사 목록 조회 기능
    public EventResponse.PageDto findAllPage(int page, int size, String keyword) {

        // 페이지 번호 0부터 시작하도록 반환
        int pageIndex = Math.max(0, page - 1);
        // 페이지 크기 1 ~ 50
        int validSize = Math.max(1, Math.min(50, size));
        // 최신 글 먼저 오도록 id 내림차순 정렬
        Pageable pageable = PageRequest.of(pageIndex, validSize, Sort.by(Sort.Direction.DESC, "id"));
        // 행사 조회
        Page<Event> eventPage;
        if (keyword == null || keyword.isBlank()) {
            eventPage = eventRepository.findAllPublished(EventStatus.PUBLISHED, pageable);
        } else {
            eventPage = eventRepository.findAllKeyWord(EventStatus.PUBLISHED, keyword.trim(), pageable);
        }

        return new EventResponse.PageDto(eventPage);
    }

    // 행사 상세 조회 기능
    @Transactional
    public EventResponse.DetailDto findById(Long id, Long companyId) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        boolean owner = companyId != null && event.getCompany().getId().equals(companyId);

        return new EventResponse.DetailDto(event, owner);
    }

    // 행사 수정 기능
    @Transactional
    public EventResponse.UpdateFormDto update(Long id, Long companyId) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("게시글 수정 권한이 없습니다.");
        }
        return new EventResponse.UpdateFormDto(event);
    }

    // 보완 요청 - 수정 후 재제출 (수정된 내용 저장, 상태 변경, 메시지 초기화)
    @Transactional
    public Event update(Long id, Long companyId, EventRequest.UpdateDto updateDto) {
        Event event = eventRepository.findByDetail(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("게시글 수정 권한이 없습니다.");
        }
        EventCategory category = eventCategoryRepository.findById(updateDto.getCategoryId())
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));

        // 수정된 행사 내용 저장
        event = updateDto.toEntity(event, category);

        event.setStatus(EventStatus.PENDING_REVIEW);
        event.setFeedbackMessage(null);
        return event;
    }

    // 행사 삭제 기능
    @Transactional
    public void delete(Long id, Long companyId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        if (companyId == null || !event.isOwner(companyId)) {
            throw new Exception403("삭제 권한이 없습니다.");
        }
        eventRepository.delete(event);
    }

    // 행사 취소 기능
    @Transactional
    public Event cancel(Long id, EventRequest.CancelDto cancelDto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        if (EventStatus.CANCELLED.equals(event.getStatus())) {
            throw new Exception400("이미 취소된 행사입니다.");
        }
        event.setStatus(EventStatus.CANCELLED);
        event.setCancelledAt(LocalDateTime.now());
        event.setCancellationReason(cancelDto.getCancellationReason());

        return event;
    }

    // 행사 승인 기능 (관리자)
    @Transactional
    public Event approve(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("게시글을 찾을 수 없습니다."));
        if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
            throw new Exception400("승인 대기 중인 행사만 승인할 수 있습니다.");
        }
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());

        return event;
    }

    // 보완 요청 (관리자 -> 기업)
    @Transactional
    public Event requestFeedback(Long id, String feedbackMessage) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
            throw new Exception400("승인 대기 중인 행사만 보완 요청할 수 있습니다.");
        }
        event.setStatus(EventStatus.FEEDBACK_REQUIRED);
        event.setFeedbackMessage(feedbackMessage);

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
    public EventResponse.ListDto findAllByCompanyId(Long companyId) {
        List<Event> eventList = eventRepository.findAllByCompanyId(companyId);
        return new EventResponse.ListDto(eventList);
    }
}

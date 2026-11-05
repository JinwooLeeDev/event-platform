package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_category.EventCategoryRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

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
        saveDto.validate();
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



    // 행사 수정 기능


    // 행사 취소 기능


    // 행사 존재 여부 확인


    // 행사 등록 및 수정 시 유효성 검사


    // 취소 시 취소 일시·담당자·사유 저장
}

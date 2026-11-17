
package com.busan.eventplatform.event.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRepositoryTest {

    @Mock
    private EventRepository eventRepository;

    @Test
    @DisplayName("ID로 행사 상세 정보를 조회한다")
    void 행사_상세조회() {
        Event event = mock(Event.class);

        when(eventRepository.findByDetail(1L))
                .thenReturn(Optional.of(event));

        Optional<Event> result = eventRepository.findByDetail(1L);

        assertTrue(result.isPresent());
        assertSame(event, result.get());
    }

    @Test
    @DisplayName("존재하지 않는 행사 상세 조회 결과는 비어 있다")
    void 행사_상세조회_실패() {
        when(eventRepository.findByDetail(999L))
                .thenReturn(Optional.empty());

        Optional<Event> result = eventRepository.findByDetail(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("심사 대상 행사를 조회한다")
    void 심사대상_조회() {
        List<Event> events = List.of(mock(Event.class));

        when(eventRepository.findReviewTargets(EventStatus.PENDING_REVIEW))
                .thenReturn(events);

        List<Event> result =
                eventRepository.findReviewTargets(EventStatus.PENDING_REVIEW);

        assertEquals(1, result.size());
        verify(eventRepository).findReviewTargets(EventStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("공개된 행사를 조회한다")
    void 공개행사_조회() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Event> page = new PageImpl<>(List.of(mock(Event.class)));

        when(eventRepository.findAllPublished(EventStatus.PUBLISHED, pageable))
                .thenReturn(page);

        Page<Event> result =
                eventRepository.findAllPublished(EventStatus.PUBLISHED, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("키워드로 행사를 검색한다")
    void 키워드_검색() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Event> page = new PageImpl<>(List.of(mock(Event.class)));

        when(eventRepository.findAllKeyWord(
                EventStatus.PUBLISHED, "축제", pageable
        )).thenReturn(page);

        Page<Event> result = eventRepository.findAllKeyWord(
                EventStatus.PUBLISHED, "축제", pageable
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("기업별 행사 목록을 조회한다")
    void 기업별_행사조회() {
        List<Event> events = List.of(mock(Event.class));

        when(eventRepository.findAllByCompanyId(1L))
                .thenReturn(events);

        List<Event> result = eventRepository.findAllByCompanyId(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("기업별 상태별 행사 목록을 조회한다")
    void 기업별_상태별_행사조회() {
        List<Event> events = List.of(mock(Event.class));

        when(eventRepository.findAllByCompanyIdAndStatus(
                1L, EventStatus.PENDING_REVIEW
        )).thenReturn(events);

        List<Event> result = eventRepository.findAllByCompanyIdAndStatus(
                1L, EventStatus.PENDING_REVIEW
        );

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("상태별 행사 개수를 조회한다")
    void 상태별_행사개수() {
        when(eventRepository.countByStatus(EventStatus.PENDING_REVIEW))
                .thenReturn(3L);

        long result = eventRepository.countByStatus(EventStatus.PENDING_REVIEW);

        assertEquals(3L, result);
    }
}

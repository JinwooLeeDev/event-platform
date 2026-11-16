
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.event_category.EventCategory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventTest {

    @Test
    @DisplayName("행사 엔티티를 생성한다")
    void createEvent() {
        Company company = mock(Company.class);
        EventCategory category = mock(EventCategory.class);

        Event event = Event.builder()
                .id(100L)
                .company(company)
                .category(category)
                .title("부산 전시 행사")
                .summary("전시 행사 소개")
                .description("전시 행사 상세 설명")
                .address("부산광역시 해운대구")
                .status(EventStatus.PENDING_REVIEW)
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals(100L, event.getId());
        assertSame(company, event.getCompany());
        assertSame(category, event.getCategory());
        assertEquals("부산 전시 행사", event.getTitle());
        assertEquals("전시 행사 소개", event.getSummary());
        assertEquals("전시 행사 상세 설명", event.getDescription());
        assertEquals("부산광역시 해운대구", event.getAddress());
        assertEquals(EventStatus.PENDING_REVIEW, event.getStatus());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("행사 소유 기업이면 true를 반환한다")
    void isOwner_true() {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);

        Event event = Event.builder()
                .company(company)
                .build();

        assertTrue(event.isOwner(1L));
    }

    @Test
    @DisplayName("행사 소유 기업이 아니면 false를 반환한다")
    void isOwner_false() {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);

        Event event = Event.builder()
                .company(company)
                .build();

        assertFalse(event.isOwner(2L));
    }

    @Test
    @DisplayName("결제 대기 상태이면 true를 반환한다")
    void isPaidStatus_true() {
        Event event = Event.builder()
                .status(EventStatus.PAID_READY)
                .build();

        assertTrue(event.isPaidStatus());
    }

    @Test
    @DisplayName("결제 대기 상태가 아니면 false를 반환한다")
    void isPaidStatus_false() {
        Event event = Event.builder()
                .status(EventStatus.PENDING_REVIEW)
                .build();

        assertFalse(event.isPaidStatus());
    }

    @Test
    @DisplayName("승인 대기 상태이면 true를 반환한다")
    void isPendingStatus_true() {
        Event event = Event.builder()
                .status(EventStatus.PENDING_REVIEW)
                .build();

        assertTrue(event.isPendingStatus());
    }

    @Test
    @DisplayName("승인 대기 상태가 아니면 false를 반환한다")
    void isPendingStatus_false() {
        Event event = Event.builder()
                .status(EventStatus.PUBLISHED)
                .build();

        assertFalse(event.isPendingStatus());
    }

    @Test
    @DisplayName("행사 생성 시각을 자동으로 설정한다")
    void onCreate() {
        Event event = new Event();

        LocalDateTime before = LocalDateTime.now();

        event.onCreate();

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(event.getCreatedAt());
        assertFalse(event.getCreatedAt().isBefore(before));
        assertFalse(event.getCreatedAt().isAfter(after));
    }
}

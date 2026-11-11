
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventTest {

    // 모집 마감 전이면 false
    @Test
    void isApplicationClosed_beforeEnd_test() {
        Event event = Event.builder()
                .applyEndAt(LocalDateTime.now().plusDays(1))
                .build();

        assertFalse(event.isApplicationClosed());
    }

    // 모집 마감 시간이 지나면 true
    @Test
    void isApplicationClosed_afterEnd_test() {
        Event event = Event.builder()
                .applyEndAt(LocalDateTime.now().minusDays(1))
                .build();

        assertTrue(event.isApplicationClosed());
    }

    // 모집 마감 시간이 현재 시간이면 true
    @Test
    void isApplicationClosed_atEnd_test() {
        Event event = Event.builder()
                .applyEndAt(LocalDateTime.now())
                .build();

        assertTrue(event.isApplicationClosed());
    }

    // 기업 소유자 확인
    @Test
    void isOwner_test() {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);

        Event event = Event.builder()
                .company(company)
                .build();

        assertTrue(event.isOwner(1L));
        assertFalse(event.isOwner(2L));
    }

    // 행사 생성 시 생성 시각 자동 입력
    @Test
    void onCreate_test() {
        Event event = Event.builder().build();

        event.onCreate();

        assertNotNull(event.getCreatedAt());
    }
}

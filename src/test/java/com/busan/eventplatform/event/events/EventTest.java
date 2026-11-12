package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventTest {

    // 1. 행사 생성 시 소유 기업 확인
    @Test
    void isOwner_test() {
        // given
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);

        Event event = Event.builder()
                .company(company)
                .build();

        // when
        boolean result = event.isOwner(1L);

        // then
        assertThat(result).isTrue();
    }

    // 2. 다른 기업은 소유자가 아님
    @Test
    void isOwner_differentCompany_test() {
        // given
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);

        Event event = Event.builder()
                .company(company)
                .build();

        // when
        boolean result = event.isOwner(2L);

        // then
        assertThat(result).isFalse();
    }

    // 3. 행사 생성 시각 자동 설정
    @Test
    void onCreate_test() {
        // given
        Event event = new Event();

        // when
        event.onCreate();

        // then
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // 4. 행사 생성 시각이 기존 값으로 덮어지지 않는지 확인
    @Test
    void onCreate_setsCurrentTime_test() {
        // given
        Event event = new Event();

        // when
        event.onCreate();

        LocalDateTime createdAt = event.getCreatedAt();

        // then
        assertThat(createdAt).isNotNull();
    }
}

package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.event.EventSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventCancelTest {

    @Test
    void 취소신청_생성시_기본값이_설정된다() {
        // given
        EventCancel eventCancel = EventCancel.builder()
                .eventSession(new EventSession())
                .reason("행사 운영이 어려워졌습니다.")
                .build();

        // when
        eventCancel.onCreate();

        // then
        assertEquals(EventCancelStatus.PENDING, eventCancel.getStatus());
        assertNotNull(eventCancel.getCreatedAt());
    }

    @Test
    void 취소신청_승인() {
        // given
        EventCancel eventCancel = EventCancel.builder()
                .status(EventCancelStatus.PENDING)
                .build();

        // when
        eventCancel.approve();

        // then
        assertEquals(EventCancelStatus.APPROVED, eventCancel.getStatus());
    }

    @Test
    void 취소신청_거절() {
        // given
        EventCancel eventCancel = EventCancel.builder()
                .status(EventCancelStatus.PENDING)
                .build();

        // when
        eventCancel.reject();

        // then
        assertEquals(EventCancelStatus.REJECTED, eventCancel.getStatus());
    }

    @Test
    void 취소신청_생성시각이_자동으로_설정된다() {
        // given
        EventCancel eventCancel = EventCancel.builder()
                .eventSession(new EventSession())
                .reason("행사 운영이 어려워졌습니다.")
                .build();

        LocalDateTime before = LocalDateTime.now();

        // when
        eventCancel.onCreate();

        LocalDateTime after = LocalDateTime.now();

        // then
        assertNotNull(eventCancel.getCreatedAt());
        assertFalse(eventCancel.getCreatedAt().isBefore(before));
        assertFalse(eventCancel.getCreatedAt().isAfter(after));
    }
}

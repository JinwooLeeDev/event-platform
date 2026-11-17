
package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCancelServiceTest {

    @Mock
    private EventCancelRepository eventCancelRepository;

    @Mock
    private EventSessionRepository eventSessionRepository;

    @InjectMocks
    private EventCancelService eventCancelService;

    // 1. 정상적인 취소 신청
    @Test
    void save_test() {

        Long companyId = 1L;
        Long sessionId = 10L;
        String reason = "행사 운영이 어려워 취소를 신청합니다.";

        EventSession eventSession = mock(EventSession.class);

        when(eventSessionRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.of(eventSession));

        eventCancelService.save(companyId, sessionId, reason);

        ArgumentCaptor<EventCancel> captor =
                ArgumentCaptor.forClass(EventCancel.class);

        verify(eventCancelRepository).save(captor.capture());

        EventCancel eventCancel = captor.getValue();

        assertEquals(eventSession, eventCancel.getEventSession());
        assertEquals(reason, eventCancel.getReason());
        assertEquals(EventCancelStatus.PENDING, eventCancel.getStatus());
    }

    // 2. 존재하지 않는 회차 신청
    @Test
    void save_fail_not_found_test() {

        Long companyId = 1L;
        Long sessionId = 10L;

        when(eventSessionRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.empty());

        Exception400 exception = assertThrows(
                Exception400.class,
                () -> eventCancelService.save(companyId, sessionId, "취소 사유")
        );

        assertEquals("회차를 찾을 수 없습니다.", exception.getMessage());

        verify(eventCancelRepository, never()).save(any(EventCancel.class));
    }

    // 3. 다른 기업 소유의 회차 신청 차단
    @Test
    void save_fail_other_company_test() {

        Long companyId = 2L;
        Long sessionId = 10L;

        when(eventSessionRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception400.class,
                () -> eventCancelService.save(companyId, sessionId, "취소 사유")
        );

        verify(eventCancelRepository, never()).save(any(EventCancel.class));
    }

    // 4. 취소 신청 상태가 PENDING인지 확인
    @Test
    void save_pending_status_test() {

        EventSession eventSession = mock(EventSession.class);

        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
                .thenReturn(Optional.of(eventSession));

        eventCancelService.save(1L, 10L, "행사 취소 사유");

        ArgumentCaptor<EventCancel> captor =
                ArgumentCaptor.forClass(EventCancel.class);

        verify(eventCancelRepository).save(captor.capture());

        assertEquals(
                EventCancelStatus.PENDING,
                captor.getValue().getStatus()
        );
    }

    // 5. 취소 신청서가 한 번만 저장되는지 확인
    @Test
    void save_once_test() {

        EventSession eventSession = mock(EventSession.class);

        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
                .thenReturn(Optional.of(eventSession));

        eventCancelService.save(1L, 10L, "행사 취소 사유");

        verify(eventCancelRepository, times(1))
                .save(any(EventCancel.class));
    }
}

package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCancelServiceTest {

    @Mock
    private EventCancelRepository eventCancelRepository;

    @Mock
    private EventSessionRepository eventSessionRepository;

    @Mock
    private RefundService refundService;

    @InjectMocks
    private EventCancelService eventCancelService;

    private EventSession eventSession;

    @BeforeEach
    void setUp() {
        eventSession = mock(EventSession.class);
        lenient().when(eventSession.getStatus()).thenReturn(EventSessionStatus.SCHEDULED);
    }

    @Test
    void 회차_취소신청_성공() {
        // given
        Long companyId = 1L;
        Long sessionId = 10L;
        String reason = "행사 운영이 어려워졌습니다.";

        when(eventSessionRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.of(eventSession));

        when(eventCancelRepository.existsBySessionIdAndStatus(
                sessionId, EventCancelStatus.PENDING
        )).thenReturn(false);

        // when
        eventCancelService.save(companyId, sessionId, reason);

        // then
        ArgumentCaptor<EventCancel> captor =
                ArgumentCaptor.forClass(EventCancel.class);

        verify(eventCancelRepository).save(captor.capture());

        EventCancel saved = captor.getValue();

        assertEquals(eventSession, saved.getEventSession());
        assertEquals(reason, saved.getReason());
        assertEquals(EventCancelStatus.PENDING, saved.getStatus());
    }

    @Test
    void 소유하지_않은_회차는_취소신청할_수_없다() {
        // given
        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception400.class, () ->
                eventCancelService.save(1L, 10L, "취소 사유")
        );

        verify(eventCancelRepository, never()).save(any());
    }

    @Test
    void 이미_대기중인_취소신청이_있으면_중복신청할_수_없다() {
        // given
        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
                .thenReturn(Optional.of(eventSession));

        when(eventCancelRepository.existsBySessionIdAndStatus(
                10L, EventCancelStatus.PENDING
        )).thenReturn(true);

        // when & then
        assertThrows(Exception400.class, () ->
                eventCancelService.save(1L, 10L, "취소 사유")
        );

        verify(eventCancelRepository, never()).save(any());
    }

    @Test
    void 취소신청_승인시_회차취소와_환불을_처리한다() {
        // given
        Long cancelId = 100L;
        Long sessionId = 10L;

        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 17, 10, 0);
        String reason = "행사 운영이 어려워졌습니다.";

        EventCancel eventCancel = mock(EventCancel.class);
        Event event = mock(Event.class);
        Company company = mock(Company.class);

        when(eventCancelRepository.findById(cancelId))
                .thenReturn(Optional.of(eventCancel));

        when(eventCancel.getStatus())
                .thenReturn(EventCancelStatus.PENDING);

        when(eventCancel.getEventSession())
                .thenReturn(eventSession);

        when(eventCancel.getCreatedAt())
                .thenReturn(createdAt);

        when(eventCancel.getReason())
                .thenReturn(reason);

        when(eventSession.getId())
                .thenReturn(sessionId);

        when(eventSession.getEvent())
                .thenReturn(event);

        when(event.getCompany())
                .thenReturn(company);

        // when
        eventCancelService.approve(cancelId);

        // then
        verify(eventSession).setStatus(EventSessionStatus.CANCELLED);

        verify(refundService).pointBackUser(eventSession);

        verify(refundService).refundCompany(
                company,
                sessionId,
                LocalDate.of(2026, 11, 17),
                reason
        );

        verify(eventCancel).approve();
    }

    @Test
    void 이미_처리된_취소신청은_승인할_수_없다() {
        // given
        EventCancel eventCancel = mock(EventCancel.class);

        when(eventCancelRepository.findById(100L))
                .thenReturn(Optional.of(eventCancel));

        when(eventCancel.getStatus())
                .thenReturn(EventCancelStatus.APPROVED);

        // when & then
        assertThrows(Exception400.class, () ->
                eventCancelService.approve(100L)
        );

        verify(refundService, never()).pointBackUser(any());
        verify(refundService, never()).refundCompany(
                any(), any(), any(), any()
        );
    }

    @Test
    void 존재하지_않는_취소신청은_승인할_수_없다() {
        // given
        when(eventCancelRepository.findById(100L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception400.class, () ->
                eventCancelService.approve(100L)
        );
    }

    @Test
    void 취소신청_거절_성공() {
        // given
        EventCancel eventCancel = mock(EventCancel.class);

        when(eventCancelRepository.findById(100L))
                .thenReturn(Optional.of(eventCancel));

        when(eventCancel.getStatus())
                .thenReturn(EventCancelStatus.PENDING);

        // when
        eventCancelService.reject(100L);

        // then
        verify(eventCancel).reject();
    }

    @Test
    void 이미_처리된_취소신청은_거절할_수_없다() {
        // given
        EventCancel eventCancel = mock(EventCancel.class);

        when(eventCancelRepository.findById(100L))
                .thenReturn(Optional.of(eventCancel));

        when(eventCancel.getStatus())
                .thenReturn(EventCancelStatus.REJECTED);

        // when & then
        assertThrows(Exception400.class, () ->
                eventCancelService.reject(100L)
        );

        verify(eventCancel, never()).reject();
    }
}

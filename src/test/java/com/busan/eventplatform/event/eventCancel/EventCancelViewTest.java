package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventSession.*;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventCancelViewTest {
    @Test void formRendersAfterDatabaseSessionCloses() throws Exception {
        var repository = mock(EventCancelRepository.class);
        var sessions = mock(EventSessionRepository.class);
        var service = new EventCancelService(repository, sessions, mock(com.busan.eventplatform.api.refund.RefundService.class));
        var databaseSessionOpen = new java.util.concurrent.atomic.AtomicBoolean(true);
        var event = mock(com.busan.eventplatform.event.events.Event.class);
        when(event.getTitle()).thenAnswer(invocation -> {
            if (!databaseSessionOpen.get()) throw new org.hibernate.LazyInitializationException("Database session closed before rendering");
            return "Host event title";
        });
        var session = EventSession.builder().id(10L).name("Session A").event(event)
                .status(EventSessionStatus.SCHEDULED).build();
        when(sessions.findByCompanyIdAndSessionId(1L, 10L)).thenReturn(Optional.of(session));
        var model = new java.util.HashMap<>(service.context(1L, 10L));
        model.put("sessionId", 10L);
        databaseSessionOpen.set(false);
        String html = AdminEventCancelViewTest.render("event_cancel/save-form", model);
        assertTrue(html.contains("Host event title"));
        assertTrue(html.contains("action=\"/event-cancels/save/10\""));
    }

    @Test void terminalAndRunningSessionsCannotSubmitNewRequests() {
        for (var status : new EventSessionStatus[]{EventSessionStatus.IN_PROGRESS, EventSessionStatus.COMPLETED, EventSessionStatus.CANCELLED}) {
            var repository = mock(EventCancelRepository.class);
            var sessions = mock(EventSessionRepository.class);
            var service = new EventCancelService(repository, sessions, mock(com.busan.eventplatform.api.refund.RefundService.class));
            when(sessions.findByCompanyIdAndSessionId(1L, 10L)).thenReturn(Optional.of(
                    EventSession.builder().id(10L).status(status).build()));
            assertThrows(Exception400.class, () -> service.save(1L, 10L, "Venue unavailable"));
            verify(repository, never()).save(any());
        }
    }

    @Test void legacyDeleteRouteOnlyOpensRequestForm() {
        var service = mock(EventCancelService.class);
        var sessions = mock(EventSessionService.class);
        var controller = new EventSessionController(sessions, service);
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(com.busan.eventplatform.login.LoginSession.IDENTITY,
                com.busan.eventplatform.login.LoginIdentity.builder().id(1L)
                        .loginType(com.busan.eventplatform.login.LoginType.COMPANY).build());
        session.setAttribute(com.busan.eventplatform.login.LoginSession.COMPANY,
                com.busan.eventplatform.login.companylogin.SessionCompanyDto.builder().id(1L).build());
        assertEquals("redirect:/event-cancels/save/10", controller.delete(10L, session));
        verify(service).context(1L, 10L);
        verifyNoInteractions(sessions);
    }

    @Test void formContextChecksOwnershipAndPendingRequest() {
        var repository = mock(EventCancelRepository.class);
        var sessions = mock(EventSessionRepository.class);
        var service = new EventCancelService(repository, sessions, mock(com.busan.eventplatform.api.refund.RefundService.class));
        when(sessions.findByCompanyIdAndSessionId(1L, 10L)).thenReturn(Optional.empty());
        assertThrows(Exception400.class, () -> service.context(1L, 10L));
        var session = EventSession.builder().id(10L).status(EventSessionStatus.SCHEDULED)
                .event(com.busan.eventplatform.event.events.Event.builder().title("Host event").build()).build();
        when(sessions.findByCompanyIdAndSessionId(1L, 10L)).thenReturn(Optional.of(session));
        when(repository.findFirstByEventSessionIdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(Optional.of(EventCancel.builder().status(EventCancelStatus.PENDING).build()));
        assertFalse((Boolean) service.context(1L, 10L).get("canRequestCancel"));
    }

    @Test void requestRejectsBlankAndOversizedReasonsBeforeSaving() {
        var repository = mock(EventCancelRepository.class);
        var service = new EventCancelService(repository, mock(EventSessionRepository.class), mock(com.busan.eventplatform.api.refund.RefundService.class));
        assertThrows(Exception400.class, () -> service.save(1L, 10L, "  "));
        assertThrows(Exception400.class, () -> service.save(1L, 10L, "x".repeat(256)));
        verifyNoInteractions(repository);
    }
}

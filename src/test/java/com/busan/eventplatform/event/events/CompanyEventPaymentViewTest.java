package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyEventPaymentViewTest {
    @Test void sessionSelectionAndManagementRenderExistingDtos() throws Exception {
        var event = Event.builder().id(7L).build();
        var date = LocalDateTime.of(2026, 12, 20, 10, 0);
        var session = EventSession.builder().id(12L).event(event).name("<script>session</script>")
                .startsAt(date).endsAt(date.plusHours(2)).applyStartAt(date.minusDays(5)).applyEndAt(date.minusDays(1))
                .capacity(Integer.MAX_VALUE).pricingType("FREE").status(com.busan.eventplatform.event.event_session.EventSessionStatus.SCHEDULED).build();
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        for (int count : new int[]{0, 1, 2}) {
            var rows = java.util.stream.IntStream.range(0, count).mapToObj(i -> new EventSessionResponse.EventSessionDto(session)).toList();
            try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/event_session/list.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
                var html = compiler.compile(reader).execute(java.util.Map.of("eventSession", rows, "loginUser", false));
                assertEquals(count, html.split("data-session-link", -1).length - 1);
                assertTrue(html.contains("data-session-create-form"));
                assertTrue(html.contains("name=\"applyStartAt\""));
                if (count > 0) { assertTrue(html.contains("제한 없음")); assertTrue(html.contains("&lt;script&gt;session&lt;/script&gt;")); }
            }
        }
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/event_session/detail.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            var html = compiler.compile(reader).execute(java.util.Map.of("eventSession", new EventSessionResponse.DetailDto(session), "loginUser", false));
            assertTrue(html.contains("data-participants=\"12\""));
            assertTrue(html.contains("/events/sessions/list/7?select=1"));
            assertFalse(html.contains("<script>session</script>"));
            assertFalse(html.contains("/reject/"));
            assertTrue(html.contains("action=\"/event-sessions/delete/12\""));
            assertTrue(html.contains("data-event-id=\"7\""));
        }
    }
    @Test void editFormRendersExistingValuesCategoriesAndEscapedFeedback() throws Exception {
        var category = com.busan.eventplatform.event.event_category.EventCategory.builder().id(3L).name("Culture").build();
        var event = Event.builder().id(7L).category(category).title("<script>title</script>")
                .summary("Summary").description("</textarea><script>description</script>").address("Busan")
                .feedbackMessage("<script>feedback</script>").build();
        var startsAt = LocalDateTime.of(2026, 12, 20, 10, 0);
        var session = EventSession.builder().id(12L).event(event).name("Session").startsAt(startsAt).endsAt(startsAt.plusHours(2))
                .applyStartAt(startsAt.minusDays(10)).applyEndAt(startsAt.minusDays(1)).capacity(Integer.MAX_VALUE)
                .minimumParticipants(1).minimumCheckAt(startsAt.minusHours(1)).pricingType("PAID").fee(5000)
                .holdDurationSeconds(300).reservationDepositPoints(1000).build();
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/event/update-form.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            var html = compiler.compile(reader).execute(java.util.Map.of("event", new EventResponse.UpdateFormDto(event),
                    "categoryList", List.of(category), "loginUser", false,
                    "editSessions", List.of(java.util.Map.of("index", 0, "session", new EventSessionResponse.DetailDto(session)))));
            assertTrue(html.contains("action=\"/events/update/7\" method=\"post\""));
            assertTrue(html.contains("value=\"3\" selected"));
            assertTrue(html.contains("&lt;script&gt;title&lt;/script&gt;"));
            assertTrue(html.contains("&lt;/textarea&gt;&lt;script&gt;description&lt;/script&gt;"));
            assertTrue(html.contains("&lt;script&gt;feedback&lt;/script&gt;"));
            assertFalse(html.contains("<script>feedback</script>"));
            assertTrue(html.contains("enctype=\"multipart/form-data\""));
            assertTrue(html.contains("name=\"sessions[0].id\" value=\"12\""));
            assertTrue(html.contains("name=\"sessions[0].reservationDepositPoints\" value=\"1000\""));
            assertTrue(html.contains("name=\"sessions[0].startsAt\" data-start value=\"2026-12-20T10:00\""));
            assertTrue(html.contains("src=\"/event-files/7\""));
            assertTrue(html.contains("name=\"sessions[0].applyStartAt\" data-apply-start value=\"2026-12-10T10:00\""));
            assertTrue(html.contains("name=\"sessions[0].applyEndAt\" data-apply-end value=\"2026-12-19T10:00\""));
        }
    }
    @Test void paymentActionOnlyRendersForApprovedEvents() throws Exception {
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        for (var status : List.of(EventStatus.PENDING_REVIEW, EventStatus.PAID_READY, EventStatus.PUBLISHED)) {
            var event = Event.builder().id(7L).title("Event").summary("Summary").status(status).build();
            var list = new EventResponse.ListDto(List.of(event));
            list.getEventList().getFirst().setSessionCount(2);
            var start = LocalDateTime.of(2026, 12, 20, 10, 0);
            list.getEventList().getFirst().setSessionPeriods(List.of(
                    new EventResponse.SessionPeriodDto(EventSession.builder().id(12L).name("오전 회차")
                            .applyStartAt(start.minusDays(5)).applyEndAt(start.minusDays(1)).build()),
                    new EventResponse.SessionPeriodDto(EventSession.builder().id(13L).name("오후 회차")
                            .applyStartAt(start.minusDays(5)).applyEndAt(start.minusDays(1)).build())));
            try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/company/event-list.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
                var html = compiler.compile(reader).execute(java.util.Map.of("eventList", list, "loginUser", false));
                assertEquals(status == EventStatus.PAID_READY, html.contains("action=\"/event/deposit/12\""));
                assertEquals(status == EventStatus.PAID_READY, html.contains("action=\"/event/deposit/13\""));
                assertFalse(html.contains("action=\"/event/deposit/7\""));
                assertTrue(html.contains("action=\"/events/delete/7\""));
                if (status == EventStatus.PAID_READY) {
                    assertTrue(html.contains("150,000 P"));
                    assertTrue(html.contains("오전 회차"));
                    assertTrue(html.contains("오후 회차"));
                    assertTrue(html.contains("href=\"/charge\""));
                }
            }
        }
    }
    @Test void capacityLabelPreservesFiniteCapacityAndHidesUnlimitedSentinel() {
        var session = mock(EventSession.class);
        when(session.getCapacity()).thenReturn(Integer.MAX_VALUE);
        assertEquals("제한 없음", new EventSessionResponse.EventSessionDto(session).getCapacityLabel());
        when(session.getCapacity()).thenReturn(30);
        assertEquals("30명", new EventSessionResponse.EventSessionDto(session).getCapacityLabel());
    }

    @Test void realSessionCountReachesPaymentFormAndAmount() {
        var repository = mock(EventRepository.class);
        var sessions = mock(com.busan.eventplatform.event.event_session.EventSessionRepository.class);
        var service = new EventService(repository, null, null, null, null, sessions, null);
        var event = Event.builder().id(7L).status(EventStatus.PAID_READY).build();
        var session = mock(EventSession.class);
        when(session.getEvent()).thenReturn(event);
        when(session.getApplyStartAt()).thenReturn(LocalDateTime.now());
        when(session.getApplyEndAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(repository.findAllByCompanyId(1L)).thenReturn(List.of(event));
        when(sessions.findAllByEventIds(List.of(7L))).thenReturn(List.of(session, session));
        var dto = service.findAllByCompanyId(1L).getEventList().getFirst();
        assertEquals(2, dto.getSessionCount());
        assertEquals("300,000", dto.getDepositAmountText());
        assertTrue(dto.isPaidReadyEvent());
    }
}

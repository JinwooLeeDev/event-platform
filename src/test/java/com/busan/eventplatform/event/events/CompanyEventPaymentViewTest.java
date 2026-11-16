package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyEventPaymentViewTest {
    @Test void editFormRendersExistingValuesCategoriesAndEscapedFeedback() throws Exception {
        var category = com.busan.eventplatform.event.event_category.EventCategory.builder().id(3L).name("Culture").build();
        var event = Event.builder().id(7L).category(category).title("<script>title</script>")
                .summary("Summary").description("</textarea><script>description</script>").address("Busan")
                .feedbackMessage("<script>feedback</script>").build();
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/event/update-form.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            var html = compiler.compile(reader).execute(java.util.Map.of("event", new EventResponse.UpdateFormDto(event),
                    "categoryList", List.of(category), "loginUser", false));
            assertTrue(html.contains("action=\"/events/update/7\" method=\"post\""));
            assertTrue(html.contains("value=\"3\" selected"));
            assertTrue(html.contains("&lt;script&gt;title&lt;/script&gt;"));
            assertTrue(html.contains("&lt;/textarea&gt;&lt;script&gt;description&lt;/script&gt;"));
            assertTrue(html.contains("&lt;script&gt;feedback&lt;/script&gt;"));
            assertFalse(html.contains("<script>feedback</script>"));
        }
    }
    @Test void paymentActionOnlyRendersForApprovedEvents() throws Exception {
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        for (var status : List.of(EventStatus.PENDING_REVIEW, EventStatus.PAID_READY, EventStatus.PUBLISHED)) {
            var event = Event.builder().id(7L).title("Event").summary("Summary").status(status).build();
            var list = new EventResponse.ListDto(List.of(event));
            list.getEventList().getFirst().setSessionCount(2);
            try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/company/event-list.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
                var html = compiler.compile(reader).execute(java.util.Map.of("eventList", list, "loginUser", false));
                assertEquals(status == EventStatus.PAID_READY, html.contains("action=\"/event/deposit/7\""));
                if (status == EventStatus.PAID_READY) {
                    assertTrue(html.contains("300,000 P"));
                    assertTrue(html.contains("name=\"sessionCount\" value=\"2\""));
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
        var service = new EventService(repository, null, null, null, null, sessions);
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

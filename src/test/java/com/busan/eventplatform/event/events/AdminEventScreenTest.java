package com.busan.eventplatform.event.events;

import com.busan.eventplatform.admin.*;
import com.busan.eventplatform.company.companyAdminReview.CompanyReviewService;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminEventScreenTest {
    @Test void detailRendersEscapedSubmissionAndReviewActions() throws Exception {
        var repository = mock(EventRepository.class);
        var sessions = mock(EventSessionService.class);
        var controller = new AdminEventController(repository, mock(EventService.class), sessions);
        var company = com.busan.eventplatform.company.Company.builder().name("Host").build();
        var category = mock(com.busan.eventplatform.event.event_category.EventCategory.class);
        when(category.getName()).thenReturn("Culture");
        var event = Event.builder().id(7L).title("<script>unsafe</script>").company(company).category(category)
                .summary("Summary").description("Description").address("Busan")
                .createdAt(java.time.LocalDateTime.now()).status(EventStatus.PENDING_REVIEW).build();
        when(repository.findByDetail(7L)).thenReturn(Optional.of(event));
        when(sessions.findByEventId(7L)).thenReturn(List.of());
        var model = new ExtendedModelMap();
        controller.detail(7L, model);
        model.addAttribute("loginUser", java.util.Map.of("displayName", "Admin"));
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/admin/page.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            var html = compiler.compile(reader).execute(model);
            assertTrue(html.contains("&lt;script&gt;unsafe&lt;/script&gt;"));
            assertTrue(html.contains("/admin/events/7/approve"));
            assertTrue(html.contains("/admin/events/7/feedback"));
            assertFalse(html.contains("<script>unsafe</script>"));
        }
    }
    @Test void dashboardUsesPendingCountIncludingZero() {
        var repository = mock(EventRepository.class);
        var companies = mock(CompanyReviewService.class);
        when(companies.findReviewTargets(1L)).thenReturn(List.of());
        var controller = new AdminController(companies, repository);
        for (long count : new long[]{0, 3}) {
            when(repository.countByStatus(EventStatus.PENDING_REVIEW)).thenReturn(count);
            var model = new ExtendedModelMap();
            controller.dashboard(SessionUserDto.builder().id(1L).build(), model);
            assertEquals(count, model.get("eventReviewCount"));
        }
    }

    @Test void reviewListAndProcessedDetailProvideCorrectState() {
        var repository = mock(EventRepository.class);
        var sessions = mock(EventSessionService.class);
        var controller = new AdminEventController(repository, mock(EventService.class), sessions);
        when(repository.findReviewTargets(EventStatus.PENDING_REVIEW)).thenReturn(List.of());
        var model = new ExtendedModelMap();
        assertEquals("admin/page", controller.list(model, EventStatus.PENDING_REVIEW));
        assertEquals(0, model.get("eventReviewCount"));
        when(repository.findByDetail(1L)).thenReturn(Optional.of(Event.builder().status(EventStatus.PAID_READY).build()));
        when(sessions.findByEventId(1L)).thenReturn(List.of());
        controller.detail(1L, model);
        assertEquals(false, model.get("reviewable"));
    }

    @Test void cancelValidatesReasonAndReusesExistingService() {
        var service = mock(EventService.class);
        var controller = new AdminEventController(mock(EventRepository.class), service, mock(EventSessionService.class));
        var dto = new EventRequest.CancelDto();
        dto.setCancellationReason("   ");
        var redirect = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> controller.cancel(7L, dto, redirect));
        verifyNoInteractions(service);
        dto.setCancellationReason("x".repeat(256));
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> controller.cancel(7L, dto, redirect));
        verifyNoInteractions(service);
        dto.setCancellationReason("Venue unavailable");
        assertEquals("redirect:/admin/events/7", controller.cancel(7L, dto, redirect));
        verify(service).cancel(7L, dto);
        assertTrue(redirect.getFlashAttributes().containsKey("reviewSuccess"));
    }

    @Test void publishedEventsCanBeFoundAndCancelledEventsCannotBeCancelledAgain() throws Exception {
        var repository = mock(EventRepository.class);
        var sessions = mock(EventSessionService.class);
        var controller = new AdminEventController(repository, mock(EventService.class), sessions);
        var event = Event.builder().id(7L).title("Host event").status(EventStatus.PUBLISHED)
                .company(com.busan.eventplatform.company.Company.builder().name("Host").build())
                .category(mock(com.busan.eventplatform.event.event_category.EventCategory.class))
                .summary("Summary").description("Description").address("Busan")
                .createdAt(java.time.LocalDateTime.now()).build();
        when(repository.findReviewTargets(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(event.getCategory().getName()).thenReturn("Culture");
        when(repository.findByDetail(7L)).thenReturn(Optional.of(event));
        when(sessions.findByEventId(7L)).thenReturn(List.of());
        var model = new ExtendedModelMap();
        controller.list(model, EventStatus.PUBLISHED);
        assertEquals(List.of(event), model.get("reviewEvents"));
        controller.detail(7L, model);
        assertEquals(true, model.get("cancellable"));
        assertTrue(renderEvents(model).contains("/admin/events/7/cancel"));
        event.setStatus(EventStatus.CANCELLED);
        event.setCancellationReason("<script>reason</script>");
        controller.detail(7L, model);
        var html = renderEvents(model);
        assertEquals(false, model.get("cancellable"));
        assertFalse(html.contains("/admin/events/7/cancel"));
        assertTrue(html.contains("&lt;script&gt;reason&lt;/script&gt;"));
    }

    private String renderEvents(ExtendedModelMap model) throws Exception {
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/admin/events.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            return com.samskivert.mustache.Mustache.compiler().compile(reader).execute(model);
        }
    }
}

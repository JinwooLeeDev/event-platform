package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.admin.AdminEventCancelController;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminEventCancelViewTest {
    @Test void requestFormShowsContextAndHidesSubmissionWhilePending() throws Exception {
        for (boolean canRequest : new boolean[]{true, false}) {
            var model = new HashMap<String, Object>();
            model.put("sessionId", 10L);
            model.put("canRequestCancel", canRequest);
            model.put("cancelSession", Map.of("name", "Session A", "event", Map.of("title", "Host event"),
                    "startsAt", "2026-12-01", "endsAt", "2026-12-02", "occupiedCount", 2));
            String html = render("event_cancel/save-form", model);
            assertTrue(html.contains("Host event"));
            assertTrue(html.contains("Session A"));
            assertEquals(canRequest, html.contains("action=\"/event-cancels/save/10\""));
            assertFalse(html.contains("/event-sessions/delete/"));
        }
    }

    @Test void reviewUsesExistingTransactionsAndPreservesFailure() {
        var service = mock(EventCancelService.class);
        var controller = new AdminEventCancelController(service);
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/admin/event-cancels", controller.approve(3L, redirect));
        verify(service).approve(3L);
        controller.reject(4L, redirect);
        verify(service).reject(4L);
        doThrow(new com.busan.eventplatform.global.error.exception.Exception400("Already processed"))
                .when(service).approve(5L);
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class,
                () -> controller.approve(5L, new RedirectAttributesModelMap()));
    }

    @Test void adminPageRendersEscapedReasonAndOnlyPendingActions() throws Exception {
        var service = mock(EventCancelService.class);
        var controller = new AdminEventCancelController(service);
        for (boolean pending : new boolean[]{true, false}) {
            var request = Map.of("id", 3L, "reason", "<script>alert(1)</script>", "createdAt", "2026-11-18",
                    "eventSession", Map.of("name", "Session A", "startsAt", "2026-12-01", "endsAt", "2026-12-02", "occupiedCount", 2,
                            "event", Map.of("id", 1L, "title", "Event", "company", Map.of("name", "Host"))));
            when(service.reviewRequests(EventCancelStatus.PENDING)).thenReturn(List.of(
                    Map.of("request", request, "pending", pending, "statusLabel", "Review")));
            var model = new ExtendedModelMap();
            controller.list(EventCancelStatus.PENDING, model);
            String html = render("admin/page", model);
            assertTrue(html.contains("&lt;script&gt;"));
            assertFalse(html.contains("<script>alert(1)</script>"));
            assertEquals(pending, html.contains("action=\"/admin/event-cancels/3/approve\""));
            assertFalse(html.contains("admin-empty-large"));
        }
    }

    static String render(String template, Object model) throws Exception {
        var compiler = com.samskivert.mustache.Mustache.compiler().defaultValue("")
                .withLoader(name -> Files.newBufferedReader(Path.of("src/main/resources/templates", name + ".mustache")));
        return compiler.compile(Files.readString(Path.of("src/main/resources/templates", template + ".mustache"))).execute(model);
    }
}

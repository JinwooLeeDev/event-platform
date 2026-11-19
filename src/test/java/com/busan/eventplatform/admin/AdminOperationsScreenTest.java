package com.busan.eventplatform.admin;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCategory.*;
import com.busan.eventplatform.event.eventSession.*;
import com.busan.eventplatform.event.events.Event;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminOperationsScreenTest {
    @Test void sessionPageUsesStatusAndPaginationAndEscapesEventTitle() throws Exception {
        var repository = mock(EventSessionRepository.class);
        var session = EventSession.builder().id(4L).name("Afternoon").capacity(Integer.MAX_VALUE)
                .status(EventSessionStatus.SCHEDULED).startsAt(java.time.LocalDateTime.now().plusDays(2))
                .endsAt(java.time.LocalDateTime.now().plusDays(3)).applyStartAt(java.time.LocalDateTime.now())
                .applyEndAt(java.time.LocalDateTime.now().plusDays(1))
                .event(Event.builder().id(7L).title("<script>event</script>")
                        .company(Company.builder().name("Host").build()).build()).build();
        when(repository.findForAdmin(eq(EventSessionStatus.SCHEDULED), any()))
                .thenReturn(new PageImpl<>(List.of(session), PageRequest.of(0, 20), 21));
        var model = new ExtendedModelMap();
        new AdminSessionController(repository).sessions(EventSessionStatus.SCHEDULED, 1, model);
        var html = render(model);
        assertTrue(html.contains("&lt;script&gt;event&lt;/script&gt;"));
        assertTrue(html.contains("/admin/events/7"));
        assertTrue(html.contains("제한 없음"));
        assertTrue(html.contains("page=2&amp;status=SCHEDULED"));
        assertFalse(html.contains("관리 기능을 준비하고 있습니다"));
        verify(repository).findForAdmin(EventSessionStatus.SCHEDULED, PageRequest.of(0, 20));
    }

    @Test void categoryPageRendersExistingCrudActions() throws Exception {
        var service = mock(EventCategoryService.class);
        when(service.findAllList()).thenReturn(new EventCategoryResponse.ListDto(List.of(
                EventCategory.builder().id(3L).name("<script>category</script>").build())));
        var model = new ExtendedModelMap();
        assertEquals("admin/page", new EventCategoryController(service).findAllList(model));
        var html = render(model);
        assertTrue(html.contains("&lt;script&gt;category&lt;/script&gt;"));
        assertTrue(html.contains("action=\"/admin/categories\""));
        assertTrue(html.contains("action=\"/admin/categories/update/3\""));
        assertTrue(html.contains("action=\"/admin/categories/delete/3\""));
        assertTrue(html.contains("href=\"/admin/categories\" aria-current=\"page\""));
    }

    @Test void categoryWritesReuseServiceAndRejectBlankNames() {
        var service = mock(EventCategoryService.class);
        var controller = new EventCategoryController(service);
        var save = new EventCategoryRequest.SaveDto();
        save.setName(" ");
        assertThrows(com.busan.eventplatform.global.error.exception.Exception400.class, () -> controller.save(save));
        verifyNoInteractions(service);
        save.setName("Culture");
        assertEquals("redirect:/admin/categories", controller.save(save));
        verify(service).save(save);
        var update = new EventCategoryRequest.UpdateDto();
        update.setName("Arts");
        assertEquals("redirect:/admin/categories", controller.update(3L, update));
        verify(service).update(3L, update);
        assertEquals("redirect:/admin/categories", controller.delete(3L));
        verify(service).delete(3L);
    }

    private String render(ExtendedModelMap model) throws Exception {
        model.addAttribute("loginUser", Map.of("displayName", "Admin"));
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/admin/page.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            return compiler.compile(reader).execute(model);
        }
    }
}

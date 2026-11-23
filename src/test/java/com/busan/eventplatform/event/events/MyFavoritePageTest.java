package com.busan.eventplatform.event.events;

import com.busan.eventplatform.global.i18n.I18nTestModels;

import com.busan.eventplatform.event.eventFavorite.*;
import com.busan.eventplatform.login.*;
import com.busan.eventplatform.global.error.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyFavoritePageTest {
    @Test void favoritesPageUsesCurrentUserAndRendersSavedEvent() throws Exception {
        var service = mock(EventFavoriteService.class);
        var favorite = mock(EventFavorite.class);
        when(favorite.getEvent()).thenReturn(Event.builder().id(7L).title("<script>event</script>").summary("Summary").status(EventStatus.PUBLISHED).build());
        var rows = List.of(new EventFavoriteResponse.ListDto(favorite));
        when(service.findAllByUserId(1L)).thenReturn(rows);
        var session = new MockHttpSession();
        session.setAttribute(LoginSession.IDENTITY, LoginIdentity.builder().id(1L).loginType(LoginType.USER).build());
        var model = new ExtendedModelMap();
        assertEquals("user/myfavorites", new MyFavoriteController(service).list(session, model));
        assertEquals(rows, model.get("favorites"));
        assertEquals(1, model.get("favoriteTotal"));
        model.addAttribute("loginUser", false);
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/user/myfavorites.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            var html = compiler.compile(reader).execute(I18nTestModels.withLocale(model));
            assertTrue(html.contains("href=\"/event/7\""));
            assertTrue(html.contains("data-favorite-event=\"7\""));
            assertTrue(html.contains("&lt;script&gt;event&lt;/script&gt;"));
            assertFalse(html.contains("<script>event</script>"));
        }
    }

    @Test void favoritesPageRejectsAnonymousAndCompanyAccounts() {
        var service = mock(EventFavoriteService.class);
        var controller = new MyFavoriteController(service);
        assertThrows(Exception401.class, () -> controller.list(null, new ExtendedModelMap()));
        var session = new MockHttpSession();
        session.setAttribute(LoginSession.IDENTITY, LoginIdentity.builder().id(1L).loginType(LoginType.COMPANY).build());
        assertThrows(Exception403.class, () -> controller.list(session, new ExtendedModelMap()));
        verifyNoInteractions(service);
    }
}

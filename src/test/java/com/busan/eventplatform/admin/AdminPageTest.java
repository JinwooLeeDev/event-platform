package com.busan.eventplatform.admin;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminPageTest {
    @Test
    void statusEndpointUsesPostAndRedirectsToMemberDetail() throws Exception {
        var service = org.mockito.Mockito.mock(AdminMemberService.class);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new AdminMemberController(service)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/members/7/status").param("status", "SUSPENDED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/members/7"));
        org.mockito.Mockito.verify(service).changeMemberStatus(7L, com.busan.eventplatform.user.UserStatus.SUSPENDED);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/admin/members/7/status"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isMethodNotAllowed());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/members/7/status").param("status", "INVALID"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        org.mockito.Mockito.verifyNoMoreInteractions(service);
    }

    @Test
    void rendersMemberSearchAndDetailWithEscapedValues() throws Exception {
        var service = org.mockito.Mockito.mock(AdminMemberService.class);
        var user = com.busan.eventplatform.user.User.builder().displayName("<script>member</script>")
                .emailNormalized("member@example.com").role(com.busan.eventplatform.user.Role.MEMBER).build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 7L);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(user));
        org.mockito.Mockito.when(service.findMembers("<search>", null, 1))
                .thenReturn(new AdminMemberResponse.PageDto(page, "<search>", null));
        org.mockito.Mockito.when(service.findMember(7L)).thenReturn(new AdminMemberResponse.MemberDto(user));
        var controller = new AdminMemberController(service);
        for (boolean detail : List.of(false, true)) {
            var model = new ExtendedModelMap();
            if (detail) controller.detail(7L, model);
            else controller.members("<search>", null, 1, model);
            model.addAttribute("loginUser", Map.of("displayName", "Admin"));
            var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                    getClass().getResourceAsStream("/templates/" + name + ".mustache"), StandardCharsets.UTF_8));
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/admin/page.mustache"), StandardCharsets.UTF_8)) {
                String html = compiler.compile(reader).execute(model);
                assertTrue(html.contains("href=\"/admin/members\" aria-current=\"page\""));
                assertTrue(html.contains("&lt;script&gt;member&lt;/script&gt;"));
                assertFalse(html.contains("관리 기능을 준비하고 있습니다"));
                if (!detail) assertTrue(html.contains("value=\"&lt;search&gt;\""));
                else {
                    assertTrue(html.contains("action=\"/admin/members/7/status\" method=\"post\""));
                    assertTrue(html.contains("value=\"SUSPENDED\""));
                    assertFalse(html.contains("정지 해제"));
                }
            }
        }
    }

    @Test
    void rendersEveryAdminPageWithSharedNavigationAndEscapedAccountName() throws Exception {
        var controller = new AdminController(org.mockito.Mockito.mock(com.busan.eventplatform.company.companyAdminReview.CompanyReviewService.class), org.mockito.Mockito.mock(com.busan.eventplatform.event.events.EventRepository.class));
        for (String section : List.of("dashboard", "events", "sessions")) {
            var model = new ExtendedModelMap();
            String view = switch (section) {
                case "events" -> new AdminEventController(org.mockito.Mockito.mock(com.busan.eventplatform.event.events.EventRepository.class), org.mockito.Mockito.mock(com.busan.eventplatform.event.events.EventService.class), org.mockito.Mockito.mock(com.busan.eventplatform.event.event_session.EventSessionService.class)).list(model, com.busan.eventplatform.event.events.EventStatus.PENDING_REVIEW);
                case "sessions" -> {
                    var sessions = org.mockito.Mockito.mock(com.busan.eventplatform.event.event_session.EventSessionRepository.class);
                    org.mockito.Mockito.when(sessions.findForAdmin(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                            .thenReturn(org.springframework.data.domain.Page.empty());
                    yield new AdminSessionController(sessions).sessions(null, 1, model);
                }
                default -> controller.dashboard(com.busan.eventplatform.login.personallogin.SessionUserDto.builder().id(1L).build(), model);
            };
            model.addAttribute("loginUser", Map.of("displayName", "<script>admin</script>"));
            var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                    getClass().getResourceAsStream("/templates/" + name + ".mustache"), StandardCharsets.UTF_8));
            String html;
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/" + view + ".mustache"), StandardCharsets.UTF_8)) {
                html = compiler.compile(reader).execute(model);
            }
            assertTrue(html.contains("href=\"/admin/" + section + "\" aria-current=\"page\""));
            assertTrue(html.contains("action=\"/logout\" method=\"post\""));
            assertTrue(html.contains("&lt;script&gt;admin&lt;/script&gt;"));
            assertFalse(html.contains("<script>admin</script>"));
            if (section.equals("dashboard")) {
                assertTrue(html.contains("<span>행사 결제 대기</span><strong>0</strong>"));
                assertTrue(html.contains("<span>행사 보완 요청</span><strong>0</strong>"));
                assertFalse(html.contains("집계 준비 중"));
            }
            else assertFalse(html.contains("관리 기능을 준비하고 있습니다"));
        }
        assertEquals("redirect:/admin/dashboard", controller.index());
    }
}

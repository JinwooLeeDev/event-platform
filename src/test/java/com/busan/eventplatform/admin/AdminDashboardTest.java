package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.i18n.I18nTestModels;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.company.companyAdminReview.AdminReviewResponse;
import com.busan.eventplatform.company.companyAdminReview.CompanyReviewService;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDashboardTest {
    @Test
    void showsTotalReviewCountAndFiveRecentTargetsUsingExistingService() throws Exception {
        var service = mock(CompanyReviewService.class);
        var targets = LongStream.rangeClosed(1, 8).mapToObj(id -> {
            var company = Company.builder().name("기업" + id).businessNumber("0000000001").status(CompanyStatus.PENDING).build();
            ReflectionTestUtils.setField(company, "id", id);
            company.recordBusinessVerification(id == 1 ? BusinessVerificationStatus.ERROR : BusinessVerificationStatus.MATCHED);
            return new AdminReviewResponse.ListDTO(company);
        }).toList();
        when(service.findReviewTargets(1L)).thenReturn(targets);
        var model = new ExtendedModelMap();
        var events = mock(com.busan.eventplatform.event.events.EventRepository.class);
        when(events.countByStatus(com.busan.eventplatform.event.events.EventStatus.PAID_READY)).thenReturn(7L);
        when(events.countByStatus(com.busan.eventplatform.event.events.EventStatus.FEEDBACK_REQUIRED)).thenReturn(2L);
        var controller = new AdminController(service, events);
        var admin = SessionUserDto.builder().id(1L).role(Role.ADMIN).build();
        assertEquals("admin/page", controller.dashboard(admin, model));
        assertEquals(8, model.get("companyReviewCount"));
        assertEquals(5, ((List<?>)model.get("companyReviewItems")).size());
        assertTrue(model.get("companyReviewItems").toString().contains("검증 오류"));
        verify(service).findReviewTargets(1L);
        model.addAttribute("loginUser", java.util.Map.of("displayName", "관리자"));
        var compiler = com.samskivert.mustache.Mustache.compiler().withLoader(name -> new java.io.InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), java.nio.charset.StandardCharsets.UTF_8));
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/templates/admin/page.mustache"), java.nio.charset.StandardCharsets.UTF_8)) {
            String html = compiler.compile(reader).execute(I18nTestModels.withLocale(model));
            assertTrue(html.contains("<span>기업 승인 대기</span><strong>8</strong>"));
            assertTrue(html.contains("href=\"/admin/companies/1\""));
            assertFalse(html.contains("href=\"/admin/companies/6\""));
            assertTrue(html.contains("검증 오류"));
            assertTrue(html.contains("<span>행사 결제 대기</span><strong>7</strong>"));
            assertTrue(html.contains("<span>행사 보완 요청</span><strong>2</strong>"));
            assertTrue(html.contains("href=\"/admin/events?status=PAID_READY\""));
            assertTrue(html.contains("href=\"/admin/events?status=FEEDBACK_REQUIRED\""));
            assertFalse(html.contains("오늘 개최 회차"));
            assertFalse(html.contains("참가 확정 인원"));
        }
    }
}

package com.busan.eventplatform.admin;

import com.busan.eventplatform.company.companyAdminReview.CompanyAdminReviewController;
import com.busan.eventplatform.company.companyAdminReview.CompanyReviewService;
import com.busan.eventplatform.company.companyAdminReview.AdminReviewResponse;
import com.busan.eventplatform.company.companyAdminReview.AdminReviewRequest;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.global.error.interceptor.AdminInterceptor;
import com.busan.eventplatform.global.error.exception.GlobalExceptionHandler;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.io.ByteArrayResource;
import com.samskivert.mustache.Mustache;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminCompanyPageTest {
    @Test
    void rendersListDetailAndPermissionsUsingTeamDto() throws Exception {
        var service = mock(CompanyReviewService.class);
        var mvc = mvc(service);
        var company = company(CompanyStatus.PENDING, BusinessVerificationStatus.MATCHED);
        when(service.findReviewTargets(1L)).thenReturn(List.of(new AdminReviewResponse.ListDTO(company)));
        var list = mvc.perform(get("/admin/companies").session(adminSession()))
                .andExpect(view().name("admin/page")).andReturn();
        String listHtml = render(list.getModelAndView().getModel());
        assertTrue(listHtml.contains("href=\"/admin/companies\" aria-current=\"page\""));
        assertTrue(listHtml.contains("href=\"/admin/companies/10\""));
        assertTrue(listHtml.contains("&lt;script&gt;기업&lt;/script&gt;"));
        assertTrue(listHtml.contains("정보 일치"));
        assertFalse(listHtml.contains("관리 기능을 준비하고 있습니다"));
        for (var status : List.of(BusinessVerificationStatus.MATCHED, BusinessVerificationStatus.ERROR)) {
            company.recordBusinessVerification(status);
            when(service.findDetail(10L, 1L)).thenReturn(new AdminReviewResponse.DetailDTO(company));
            var detail = mvc.perform(get("/admin/companies/10").session(adminSession()))
                    .andExpect(view().name("admin/page")).andReturn();
            String html = render(detail.getModelAndView().getModel());
            assertEquals(status == BusinessVerificationStatus.MATCHED, html.contains("action=\"/admin/companies/10/approve\""));
            assertTrue(html.contains("action=\"/admin/companies/10/correction\""));
            assertTrue(html.contains("name=\"feedback\""));
            assertTrue(html.contains("maxlength=\"2000\""));
            assertTrue(html.contains("href=\"/admin/companies/10/certificate\""));
        }
        ReflectionTestUtils.setField(company, "status", CompanyStatus.APPROVED);
        when(service.findDetail(10L, 1L)).thenReturn(new AdminReviewResponse.DetailDTO(company));
        String processed = render(mvc.perform(get("/admin/companies/10").session(adminSession()))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel());
        assertFalse(processed.contains("action=\"/admin/companies/10/approve\""));
        assertFalse(processed.contains("action=\"/admin/companies/10/correction\""));
        assertTrue(processed.contains("현재 심사 대상 상태가 아닙니다"));
    }

    @Test
    void approvalCorrectionAndCertificateKeepExistingServiceAndRoutes() throws Exception {
        var service = mock(CompanyReviewService.class);
        var mvc = mvc(service);
        mvc.perform(post("/admin/companies/10/approve").session(adminSession()))
                .andExpect(redirectedUrl("/admin/companies/10"));
        verify(service).approve(10L, 1L);
        mvc.perform(post("/admin/companies/10/correction").session(adminSession()).param("feedback", "증빙 보완"))
                .andExpect(redirectedUrl("/admin/companies/10"));
        verify(service).requestCorrection(eq(10L), eq(1L), argThat(request -> request.getFeedback().equals("증빙 보완")));
        when(service.findCertificate(10L,1L)).thenReturn(new ByteArrayResource(new byte[]{1,2,3}) {
            @Override public String getFilename() { return "certificate.pdf"; }
        });
        mvc.perform(get("/admin/companies/10/certificate").session(adminSession()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        verify(service).findCertificate(10L,1L);
    }

    @Test
    void anonymousMemberAndCompanyAreBlockedFromReviewEndpoints() throws Exception {
        var service = mock(CompanyReviewService.class);
        var mvc = mvc(service);
        mvc.perform(get("/admin/companies")).andExpect(status().isUnauthorized());
        var member = adminSession();
        member.setAttribute(LoginSession.USER,SessionUserDto.builder().id(1L).role(Role.MEMBER).build());
        mvc.perform(get("/admin/companies/10").session(member)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/companies/10/approve").session(member)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/companies/10/correction").session(member).param("feedback","보완"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/companies/10/certificate").session(member)).andExpect(status().isForbidden());
        var corporate = adminSession();
        corporate.setAttribute(LoginSession.IDENTITY,LoginIdentity.builder().id(1L).loginType(LoginType.COMPANY).build());
        mvc.perform(get("/admin/companies").session(corporate)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private MockMvc mvc(CompanyReviewService service) {
        return MockMvcBuilders.standaloneSetup(new AdminController(service),new CompanyAdminReviewController(service))
                .addInterceptors(new AdminInterceptor()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Company company(CompanyStatus status, BusinessVerificationStatus verification) {
        var company = Company.builder().name("<script>기업</script>").email("company@example.test")
                .businessNumber("0000000010").representativeName("대표").openedOn(LocalDate.of(2020,1,1))
                .address("부산").contactPersonName("담당").contactPersonPhone("010-0000-0010").status(status).build();
        ReflectionTestUtils.setField(company,"id",10L);
        company.recordBusinessVerification(verification);
        return company;
    }

    private String render(Map<String,Object> original) throws Exception {
        var model = new HashMap<>(original);
        model.put("loginUser",Map.of("displayName","관리자"));
        var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                getClass().getResourceAsStream("/templates/"+name+".mustache"),StandardCharsets.UTF_8));
        try(var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/admin/page.mustache"),StandardCharsets.UTF_8)) {
            return compiler.compile(reader).execute(model);
        }
    }

    @Test
    void companyListHasOneMappingAndUsesExistingAdminLayout() throws Exception {
        var service = mock(CompanyReviewService.class);
        when(service.findReviewTargets(1L)).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new AdminController(service), new CompanyAdminReviewController(service)).build();
        mvc.perform(get("/admin/companies").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/page"))
                .andExpect(model().attribute("companiesPage", true))
                .andExpect(model().attribute("companyListPage", true));
        verify(service).findReviewTargets(1L);
    }

    private MockHttpSession adminSession() {
        var session = new MockHttpSession();
        session.setAttribute(LoginSession.IDENTITY, LoginIdentity.builder().id(1L).loginType(LoginType.USER).build());
        session.setAttribute(LoginSession.USER, SessionUserDto.builder().id(1L).displayName("관리자").role(Role.ADMIN).build());
        return session;
    }
}

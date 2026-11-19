package com.busan.eventplatform.admin;

import com.busan.eventplatform.company.*;
import com.busan.eventplatform.company.companyAdminReview.*;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.user.*;
import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyReviewHistoryPageTest {
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyReviewHistoryRepository histories = mock(CompanyReviewHistoryRepository.class);
    private final CompanyReviewService service = new CompanyReviewService(companies, users, histories, mock(FileStorage.class));

    @Test
    void reusesCompanyHistoryQueryAndRendersStatesReviewerAndEscapedReason() throws Exception {
        var admin = User.builder().displayName("심사 관리자").role(Role.ADMIN).status(UserStatus.ACTIVE).build();
        var company = company();
        var history = CompanyReviewHistory.builder().company(company).reviewer(admin)
                .fromStatus(CompanyStatus.PENDING).toStatus(CompanyStatus.FEEDBACK_NEEDED)
                .feedback("<script>서류 보완</script>").build();
        ReflectionTestUtils.setField(history, "createdAt", Timestamp.valueOf("2026-11-19 09:00:00"));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(companies.findById(10L)).thenReturn(Optional.of(company));
        when(histories.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(history));

        var detail = service.findDetail(10L, 1L);
        var model = new ExtendedModelMap();
        AdminCompanyView.detail(model, detail);
        String html = render(model);

        verify(histories).findByCompanyIdOrderByCreatedAtDesc(10L);
        assertTrue(html.contains("심사 이력"));
        assertTrue(html.contains("승인 대기"));
        assertTrue(html.contains("보완 요청"));
        assertTrue(html.contains("심사 관리자"));
        assertTrue(html.contains("2026-11-19"));
        assertTrue(html.contains("&lt;script&gt;서류 보완&lt;/script&gt;"));
        assertFalse(html.contains("<script>서류 보완</script>"));
    }

    @Test
    void showsNoRecordedHistoryForLegacyCompanyDetails() throws Exception {
        var model = new ExtendedModelMap();
        AdminCompanyView.detail(model, new AdminReviewResponse.DetailDTO(company()));
        assertTrue(render(model).contains("저장된 심사 이력이 없습니다."));
    }

    @Test
    void blocksNonAdminBeforeQueryingCompanyHistory() {
        var member = User.builder().role(Role.MEMBER).status(UserStatus.ACTIVE).build();
        when(users.findById(2L)).thenReturn(Optional.of(member));
        assertThrows(com.busan.eventplatform.global.error.exception.Exception403.class, () -> service.findDetail(10L, 2L));
        verifyNoInteractions(companies, histories);
    }

    private String render(ExtendedModelMap model) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/admin/companies.mustache"), StandardCharsets.UTF_8)) {
            return Mustache.compiler().compile(reader).execute(model);
        }
    }

    private Company company() {
        var company = Company.builder().name("기업").email("company@example.com").businessNumber("1234567890")
                .representativeName("대표").address("부산").contactPersonName("담당자").contactPersonPhone("010-1234-5678").build();
        ReflectionTestUtils.setField(company, "id", 10L);
        return company;
    }
}

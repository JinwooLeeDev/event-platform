package com.busan.eventplatform.company;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDetailRenderTest {
    @ParameterizedTest
    @EnumSource(CompanyStatus.class)
    void companyCanRenderOwnInformationWithoutAdminReviewFields(CompanyStatus status) throws Exception {
        var company = Company.builder().name("<script>기업</script>").email("company@example.test")
                .businessNumber("0000000001").representativeName("대표")
                .address("부산").contactPersonName("담당자").contactPersonPhone("010-0000-0001").status(status).build();
        var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), StandardCharsets.UTF_8));
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/company/detail.mustache"), StandardCharsets.UTF_8)) {
            String html = compiler.compile(reader).execute(Map.of("company", new CompanyResponse.DetailDTO(company), "loginUser", false));
            assertTrue(html.contains("&lt;script&gt;기업&lt;/script&gt;"));
            assertTrue(html.contains("company@example.test"));
            assertTrue(html.contains("href=\"/company/update\""));
            assertFalse(html.contains("/admin/companies/"));
            assertFalse(html.contains("correctionDialog"));
            assertFalse(html.contains("기업 승인"));
        }
    }
}

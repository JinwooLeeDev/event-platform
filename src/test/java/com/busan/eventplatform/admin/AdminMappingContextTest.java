package com.busan.eventplatform.admin;

import com.busan.eventplatform.company.companyAdminReview.CompanyAdminReviewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("h2")
@SpringBootTest(properties = {
        "spring.config.import=", "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:admin-mapping;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.mail.from=test@example.test", "spring.mail.host=localhost",
        "spring.mail.username=test", "spring.mail.password=test",
        "app.business-verification.service-key=test",
        "portone.store-id=test", "portone.channel-key=test", "portone.secret-key=test",
        "naver-map.client-id=test", "naver-map.client-secret=test"
})
class AdminMappingContextTest {
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void applicationStartsWithOneCompanyListMappingOwnedByReviewController() {
        var handlers = mappings.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getPatternValues().contains("/admin/companies"))
                .filter(entry -> entry.getKey().getMethodsCondition().getMethods().contains(RequestMethod.GET))
                .toList();
        assertEquals(1, handlers.size());
        assertEquals(CompanyAdminReviewController.class, handlers.getFirst().getValue().getBeanType());
    }
}

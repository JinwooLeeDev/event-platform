package com.busan.eventplatform.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import com.busan.eventplatform.company.companyAdminReview.CompanyReviewService;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminController {
    private final CompanyReviewService companyReviewService;
    private final com.busan.eventplatform.event.events.EventRepository eventRepository;
    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@SessionAttribute(LoginSession.USER) SessionUserDto admin, Model model) {
        var companies = companyReviewService.findReviewTargets(admin.getId());
        model.addAttribute("companyReviewCount", companies.size());
        model.addAttribute("eventReviewCount", eventRepository.countByStatus(com.busan.eventplatform.event.events.EventStatus.PENDING_REVIEW));
        model.addAttribute("eventPaymentCount", eventRepository.countByStatus(com.busan.eventplatform.event.events.EventStatus.PAID_READY));
        model.addAttribute("eventFeedbackCount", eventRepository.countByStatus(com.busan.eventplatform.event.events.EventStatus.FEEDBACK_REQUIRED));
        model.addAttribute("companyReviewItems", AdminCompanyView.rows(companies.stream().limit(5).toList()));
        return page(model, "운영 대시보드", "플랫폼 운영 현황과 처리할 업무를 확인하세요.", "dashboard");
    }

    static String page(Model model, String title, String description, String section) {
        return AdminPageView.prepare(model, title, description, section);
    }
}

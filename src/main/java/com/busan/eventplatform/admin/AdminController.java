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
    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@SessionAttribute(LoginSession.USER) SessionUserDto admin, Model model) {
        var companies = companyReviewService.findReviewTargets(admin.getId());
        model.addAttribute("companyReviewCount", companies.size());
        model.addAttribute("companyReviewItems", AdminCompanyView.rows(companies.stream().limit(5).toList()));
        return page(model, "운영 대시보드", "플랫폼 운영 현황과 처리할 업무를 확인하세요.", "dashboard");
    }

    @GetMapping("/admin/events")
    public String events(Model model) {
        return page(model, "행사 관리", "행사 심사와 보완 요청을 관리하는 공간입니다.", "events");
    }

    @GetMapping("/admin/sessions")
    public String sessions(Model model) {
        return page(model, "회차 현황", "회차별 모집·참가·출석 현황을 확인하는 공간입니다.", "sessions");
    }

    static String page(Model model, String title, String description, String section) {
        return AdminPageView.prepare(model, title, description, section);
    }
}

package com.busan.eventplatform.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        return page(model, "운영 대시보드", "플랫폼 운영 현황과 처리할 업무를 확인하세요.", "dashboard");
    }

    @GetMapping("/admin/companies")
    public String companies(Model model) {
        return page(model, "기업 관리", "기업 가입 정보와 사업자 증빙을 검토하는 공간입니다.", "companies");
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
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageDescription", description);
        model.addAttribute("dashboardPage", section.equals("dashboard"));
        model.addAttribute("companiesPage", section.equals("companies"));
        model.addAttribute("eventsPage", section.equals("events"));
        model.addAttribute("sessionsPage", section.equals("sessions"));
        model.addAttribute("membersPage", section.equals("members"));
        return "admin/page";
    }
}

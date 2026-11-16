package com.busan.eventplatform.admin;

import org.springframework.ui.Model;

/** 서로 다른 도메인의 관리자 화면에 공통 레이아웃 정보를 전달합니다. */
public final class AdminPageView {
    private AdminPageView() {}

    public static String prepare(Model model, String title, String description, String section) {
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

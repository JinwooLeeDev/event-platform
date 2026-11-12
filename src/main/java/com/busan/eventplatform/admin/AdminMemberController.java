package com.busan.eventplatform.admin;

import com.busan.eventplatform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminMemberController {
    private final AdminMemberService memberService;

    @PostMapping("/admin/members/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        memberService.changeMemberStatus(id, status);
        return "redirect:/admin/members/" + id;
    }

    @GetMapping("/admin/members")
    public String members(@RequestParam(defaultValue = "") String keyword,
                          @RequestParam(required = false) UserStatus status,
                          @RequestParam(defaultValue = "1") int page, Model model) {
        AdminController.page(model, "회원 관리", "회원 정보를 검색하고 계정 상태를 확인하세요.", "members");
        model.addAttribute("memberListPage", true);
        model.addAttribute("memberDetailPage", false);
        model.addAttribute("memberPage", memberService.findMembers(keyword, status, page));
        return "admin/page";
    }

    @GetMapping("/admin/members/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AdminController.page(model, "회원 상세", "회원의 기본 정보와 계정 상태를 확인하세요.", "members");
        model.addAttribute("memberListPage", false);
        model.addAttribute("memberDetailPage", true);
        model.addAttribute("member", memberService.findMember(id));
        return "admin/page";
    }
}

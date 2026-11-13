package com.busan.eventplatform.company.companyAdminReview;

import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/companies")
public class CompanyAdminReviewController {

    private final CompanyReviewService companyReviewService;

    @GetMapping
    public String list(@SessionAttribute(LoginSession.USER) SessionUserDto admin, Model model) {
        model.addAttribute("companies", companyReviewService.findReviewTargets(admin.getId()));

        return "admin/company-list";
    }

    @GetMapping("/{companyId}")
    public String detail(@PathVariable Long companyId, @SessionAttribute(LoginSession.USER) SessionUserDto admin, Model model) {
        model.addAttribute(
                "company",
                companyReviewService.findDetail(companyId, admin.getId())
        );

        return "admin/company-detail";
    }

//    기업 승인
    @PostMapping("/{companyId}/approve")
    public String approve(@PathVariable Long companyId,
                          @SessionAttribute(LoginSession.USER) SessionUserDto admin) {
        companyReviewService.approve(companyId, admin.getId());

        return "redirect:/admin/companies/" + companyId;
    }

//    기업 보완 요청
    @PostMapping("/{companyId}/correction")
    public String requestCorrection(@PathVariable Long companyId,
                                    @ModelAttribute AdminReviewRequest.Correction correction,
                                    @SessionAttribute(LoginSession.USER) SessionUserDto admin) {
        companyReviewService.requestCorrection(
                companyId,
                admin.getId(),
                correction
        );

        return "redirect:/admin/companies/" + companyId;
    }

    @GetMapping("/{companyId}/certificate")
    public ResponseEntity<Resource> certificate(@PathVariable Long companyId,
                                                @SessionAttribute(LoginSession.USER) SessionUserDto admin) {
        Resource resource = companyReviewService.findCertificate(companyId, admin.getId());

        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().build().toString())
                .header("X-Content-Type-Options", "nosniff") // 브라우저가 임의로 파일유형을 추측하지않고, Content-type을 따르게 함.
                .cacheControl(CacheControl.noStore()) // 응답을 캐시에 저장하지 않음.
                .body(resource);
    }
}

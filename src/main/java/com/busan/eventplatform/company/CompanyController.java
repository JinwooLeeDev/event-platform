package com.busan.eventplatform.company;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/company/join")
    public String joinForm() {
        return "redirect:/signup/company";
    }

    @GetMapping("/company/login")
    public String loginForm() {
        return "redirect:/login?type=company";
    }

    @GetMapping("/company/detail")
    public String detail(Model model, HttpSession session) {
        SessionCompanyDto sessionCompanyDTO = requireCompany(session);

        CompanyResponse.DetailDTO company = companyService.findByDetail(sessionCompanyDTO.getId());

        model.addAttribute("company", company);
        return "company/detail";
    }

    @GetMapping("/company/update")
    public String updateForm(Model model, HttpSession session) {
        SessionCompanyDto sessionCompanyDTO = requireCompany(session);

        CompanyResponse.UpdateDTO updateDTO = companyService.findByUpdate(sessionCompanyDTO.getId());
        model.addAttribute("company", updateDTO);
        return "company/update";
    }

    @PostMapping("/company/update")
    public String update(@ModelAttribute CompanyRequest.UpdateDTO updateDTO, HttpSession session) {
        synchronized (session) {
            SessionCompanyDto sessionCompanyDTO = requireCompany(session);

            updateDTO.validate();

            CompanyResponse.DetailDTO updatedCompany = companyService.update(sessionCompanyDTO.getId(), updateDTO, session);

            session.setAttribute(LoginSession.COMPANY, SessionCompanyDto.builder()
                    .id(sessionCompanyDTO.getId())
                    .displayName(updatedCompany.getName())
                    .build());

            // 화면에서는 이메일이 변경된 경우에만 email 필드를 전송
            if (updateDTO.getEmail() != null) {
                session.removeAttribute(Define.EMAIL_VERIFICATION);
            }

            return "redirect:/company/detail";
        }
    }
    private SessionCompanyDto requireCompany(HttpSession session) {
        LoginIdentity identity = LoginSession.requireIdentity(session);
        if (identity.getLoginType() != LoginType.COMPANY) {
            throw new Exception403("기업 계정으로 로그인해주세요.");
        }
        Object value = session.getAttribute(LoginSession.COMPANY);
        if (!(value instanceof SessionCompanyDto company)
                || !java.util.Objects.equals(identity.getId(), company.getId())) {
            throw new Exception401("기업 계정으로 다시 로그인해주세요.");
        }
        return company;
    }
}

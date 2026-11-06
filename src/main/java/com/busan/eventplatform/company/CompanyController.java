package com.busan.eventplatform.company;

import com.busan.eventplatform.global.util.Define;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/company/join")
    public String joinForm() {
        return "company/join";
    }

    // TODO 후일에 유저 로그인이랑 통일
    @PostMapping("/company/join")
    public String join(@ModelAttribute CompanyRequest.JoinDTO joinDTO, HttpSession session) {
        joinDTO.validate();
        synchronized (session) {
            CompanyResponse.JoinDTO result = companyService.join(joinDTO, session);

            session.removeAttribute(Define.EMAIL_VERIFICATION);

            return "redirect:/company/login";
        }
    }

    @GetMapping("/company/login")
    public String loginForm() {
        return "company/login";
    }

    @PostMapping("/company/login")
    public String login(@ModelAttribute CompanyRequest.LoginDTO loginDTO, HttpSession session) {
        loginDTO.validate();

        SessionCompanyDTO sessionCompanyDTO = companyService.login(loginDTO);
//        TODO Define 넣기
        session.setAttribute("companySession", sessionCompanyDTO);

        return "redirect:/";
    }

    @GetMapping("/company/detail")
    public String detail(Model model, HttpSession session) {
        //        TODO Define 넣기
        SessionCompanyDTO sessionCompanyDTO = (SessionCompanyDTO) session.getAttribute("companySession");

//        TODO 로그인 확인. 추후 인터셉터 추가
        if (sessionCompanyDTO == null) {
            return "redirect:/company/login";
        }

        CompanyResponse.DetailDTO company = companyService.findByDetail(sessionCompanyDTO.getId());

        model.addAttribute("company", company);
        return "company/detail";
    }

    @GetMapping("/company/update")
    public String updateForm(Model model, HttpSession session) {
        //        TODO Define 넣기
        SessionCompanyDTO sessionCompanyDTO = (SessionCompanyDTO) session.getAttribute("companySession");

        //        TODO 로그인 확인. 추후 인터셉터 추가
        if (sessionCompanyDTO == null) {
            return "redirect:/company/login";
        }

        CompanyResponse.UpdateDTO updateDTO = companyService.findByUpdate(sessionCompanyDTO.getId());
        model.addAttribute("company", updateDTO);
        return "company/update";
    }

    @PostMapping("/company/update")
    public String update(@ModelAttribute CompanyRequest.UpdateDTO updateDTO, HttpSession session) {
        synchronized (session) {
            //        TODO Define 넣기
            SessionCompanyDTO sessionCompanyDTO = (SessionCompanyDTO) session.getAttribute("companySession");

            //        TODO 로그인 확인. 추후 인터셉터 추가
            if (sessionCompanyDTO == null) {
                return "redirect:/company/login";
            }

            updateDTO.validate();

            SessionCompanyDTO updateSessionCompanyDTO = companyService.update(sessionCompanyDTO.getId(), updateDTO, session);

            session.setAttribute("companySession", updateSessionCompanyDTO);

            // 화면에서는 이메일이 변경된 경우에만 email 필드를 전송
            if (updateDTO.getEmail() != null) {
                session.removeAttribute(Define.EMAIL_VERIFICATION);
            }

            return "redirect:/company/detail";
        }
    }
}

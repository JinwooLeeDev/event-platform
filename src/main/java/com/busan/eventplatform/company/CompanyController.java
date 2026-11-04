package com.busan.eventplatform.company;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/company/join")
    @ResponseBody
    public ResponseEntity<?> join(CompanyRequest.JoinDTO joinDTO) {
        joinDTO.validate();

        companyService.join(joinDTO);

        return ResponseEntity.ok("회원가입에 성공");
    }
}

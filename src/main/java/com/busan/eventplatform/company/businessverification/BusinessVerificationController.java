package com.busan.eventplatform.company.businessverification;

import com.busan.eventplatform.global.i18n.LocaleMessages;
import com.busan.eventplatform.global.error.exception.UiErrorMessages;
import com.busan.eventplatform.global.error.exception.Exception400;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/business-verifications")
public class BusinessVerificationController {

    private final BusinessVerificationSessionService businessVerificationSessionService;
    private final UiErrorMessages errorMessages;

//    사업자검증 버튼 요청
    @PostMapping
    public BusinessVerificationResponse.Check check(
            @RequestBody BusinessVerificationRequest.Check check,
            HttpSession session
    ) {
        synchronized (session) {
            var result = businessVerificationSessionService.check(
                    check,
                    session
            );
            return new BusinessVerificationResponse.Check(result.getStatus(), LocaleMessages.display(result.getMessage()));
        }
    }

    // 입력 검증 실패 시 오류 화면 대신 JSON 반환
    @ExceptionHandler(Exception400.class)
    public ResponseEntity<BusinessVerificationResponse.Check> handle400 (Exception400 exception) {
        return ResponseEntity.badRequest().body(
                new BusinessVerificationResponse.Check(
                        BusinessVerificationStatus.NOT_CHECKED,
                        errorMessages.display(exception.getMessage())
                )
        );
    }
}

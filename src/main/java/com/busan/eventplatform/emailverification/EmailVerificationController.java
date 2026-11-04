package com.busan.eventplatform.emailverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception500;
import com.busan.eventplatform.global.util.Define;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    // 인증번호 발송
    @PostMapping
    public EmailVerificationResDto sendCode(@RequestBody EmailVerificationSendReqDto request, HttpSession session) {
        request.validate();

        emailVerificationService.sendCode(request.getEmail(), session);
        return new EmailVerificationResDto("인증번호가 발송되었습니다. " + Define.EMAIL_CODE_EXPIRE_MINUTES + "분 안에 입력해주세요.",
                false);
    }

    // 인증번호 확인
    @PostMapping("/confirm")
    public EmailVerificationResDto confirmCode(@RequestBody EmailVerificationConfirmReqDto request, HttpSession session) {
        request.validate();

        emailVerificationService.confirmCode(request.getEmail(), request.getCode(), session);

        return new EmailVerificationResDto("이메일 인증이 완료되었습니다.", true);
    }

    // 이메일 인증 요청은 오류 화면 대신 JSON 반환
    @ExceptionHandler(Exception400.class)
    public ResponseEntity<EmailVerificationResDto> handle400(Exception400 exception) {
        return ResponseEntity.badRequest()
                .body(new EmailVerificationResDto(
                        exception.getMessage(),
                        false
                ));
    }

    @ExceptionHandler(Exception500.class)
    public ResponseEntity<EmailVerificationResDto> handle500(Exception500 exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new EmailVerificationResDto("메일을 발송하지 못했습니다. 잠시 후 다시 시도해주세요.", false));
    }
}
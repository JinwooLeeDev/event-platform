package com.busan.eventplatform.global.error.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

// Todo - 에러 페이지 확인용 컨트롤러 예외처리 구현 완료 후 컨트롤러 클래스 삭제 필요

@Controller
public class ErrorPreviewController {

    @GetMapping("/error-preview/{status}")
    public ModelAndView preview(
            @PathVariable("status") int status
    ) {
        return switch (status) {
            case 400, 401, 403, 404, 405, 500 -> {
                ModelAndView view =
                        new ModelAndView("error/" + status);

                view.setStatus(HttpStatus.valueOf(status));
                yield view;
            }
            default -> throw new Exception404(
                    "지원하지 않는 오류 화면입니다."
            );
        };
    }
}

package com.busan.eventplatform.global.error.exception;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception400.class)
    public ModelAndView handle400(Exception400 exception) {
        return errorPage(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception401.class)
    public ModelAndView handle401(Exception401 exception) {
        return errorPage(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception403.class)
    public ModelAndView handle403(Exception403 exception) {
        return errorPage(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception404.class)
    public ModelAndView handle404(Exception404 exception) {
        return errorPage(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception405.class)
    public ModelAndView handle405(Exception405 exception) {
        return errorPage(
                HttpStatus.METHOD_NOT_ALLOWED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception500.class)
    public ModelAndView handle500(Exception500 exception) {
        log.error("서버 처리 중 오류 발생", exception);

        return errorPage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        );
    }

    /// 행사 중복 결제를 alert창으로 띄워서 막아주기 위해 추가.
    @ExceptionHandler(AlreadyPurchaseException.class)
    public ResponseEntity<String> handleAlready(AlreadyPurchaseException exception){

        String html = """
                <script>
                    alert('이미 결제된 행사입니다');
                    history.back();
                </script>
                """   ;

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8")) // 안쓰면 한글 깨짐.
                .body(html);
    }

    /// 관리자에게 행사 등록 승인을 못받은 상태
    @ExceptionHandler(NotApproveException.class)
    public ResponseEntity<String> handleApprove(NotApproveException exception){

        String html = """
                <script>
                    alert('아직 관리자에게 승인받지 못했습니다. 승인 받은 후 다시 결제해주세요');
                    history.back();
                </script>
                """   ;

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8")) // 안쓰면 한글 깨짐.
                .body(html);
    }


    private ModelAndView errorPage(
            HttpStatus status,
            String message
    ) {
        ModelAndView modelAndView = new ModelAndView("error/" + status.value());

        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("errorMessage", message);

        return modelAndView;
    }
}
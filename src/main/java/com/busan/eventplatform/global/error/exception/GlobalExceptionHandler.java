package com.busan.eventplatform.global.error.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
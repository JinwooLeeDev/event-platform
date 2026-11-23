package com.busan.eventplatform.global.error.exception;

import com.busan.eventplatform.global.i18n.LocaleMessages;
import tools.jackson.databind.json.JsonMapper;
import com.busan.eventplatform.login.LoginViewAdvice;
import com.busan.eventplatform.global.i18n.LocaleViewAdvice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final LoginViewAdvice loginViewAdvice;
    private final LocaleViewAdvice localeViewAdvice;
    private final UiErrorMessages errorMessages;

    @ExceptionHandler(Exception400.class)
    public ModelAndView handle400(Exception400 exception, HttpServletRequest request) {
        return errorPage(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception401.class)
    public ModelAndView handle401(Exception401 exception, HttpServletRequest request) {
        return errorPage(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception403.class)
    public ModelAndView handle403(Exception403 exception, HttpServletRequest request) {
        return errorPage(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception404.class)
    public ModelAndView handle404(Exception404 exception, HttpServletRequest request) {
        return errorPage(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception405.class)
    public ModelAndView handle405(Exception405 exception, HttpServletRequest request) {
        return errorPage(
                HttpStatus.METHOD_NOT_ALLOWED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception500.class)
    public ModelAndView handle500(Exception500 exception, HttpServletRequest request) {
        log.error("서버 처리 중 오류 발생", exception);

        return errorPage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                request
        );
    }

    /// 행사 중복 결제를 alert창으로 띄워서 막아주기 위해 추가.
    @ExceptionHandler(AlreadyPurchaseException.class)
    public ResponseEntity<String> handleAlready(AlreadyPurchaseException exception){

        String html = """
                <script>
                    alert(%s);
                    history.back();
                </script>
                """.formatted(javascriptString(LocaleMessages.display("이미 결제된 행사입니다")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8")) // 안쓰면 한글 깨짐.
                .body(html);
    }

    /// 관리자에게 행사 등록 승인을 못받은 상태
    @ExceptionHandler(NotApproveException.class)
    public ResponseEntity<String> handleApprove(NotApproveException exception){

        String html = """
                <script>
                    alert(%s);
                    history.back();
                </script>
                """.formatted(javascriptString(LocaleMessages.display("이미 결제되었거나 아직 관리자에게 승인받지 못했습니다. 다시 확인해주세요.")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8")) // 안쓰면 한글 깨짐.
                .body(html);
    }


    private static String javascriptString(String value) {
        return JsonMapper.builder().build().writeValueAsString(value)
                .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
                .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }

    private ModelAndView errorPage(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ModelAndView modelAndView = new ModelAndView("error/" + status.value());

        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("errorMessage", errorMessages.display(message));

        // 정상 화면과 동일한 기준으로 헤더 로그인 정보 구성
        Model loginModel = new ExtendedModelMap();
        loginViewAdvice.addLoginUser(request, loginModel);
        localeViewAdvice.addLocale(request, loginModel);

        modelAndView.addAllObjects(loginModel.asMap());

        return modelAndView;
    }
}

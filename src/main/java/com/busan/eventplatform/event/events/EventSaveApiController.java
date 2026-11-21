package com.busan.eventplatform.event.events;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class EventSaveApiController {

    private final EventService eventService;

    // 이 헤더가 있는 Fetch 요청만 처리한다.
    // 기존 일반 폼 제출은 기존 EventController가 처리한다.
    @PostMapping(
            value = "/events/save",
            headers = "X-Requested-With=XMLHttpRequest",
            produces = "application/json"
    )
    public Map<String, String> save(
            @ModelAttribute EventRequest.SaveDto saveDto,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpSession session
    ) {
        // 프론트 검증과 관계없이 서버에서 로그인·기업 권한을 확인한다.
        LoginIdentity identity = LoginSession.requireIdentity(session);

        if (!identity.isCompany()) {
            throw new Exception403("기업 회원만 행사 등록이 가능합니다.");
        }

        Object value = session.getAttribute(LoginSession.COMPANY);

        if (!(value instanceof SessionCompanyDto company)
                || !Objects.equals(identity.getId(), company.getId())) {
            throw new Exception401("기업 계정으로 다시 로그인해주세요.");
        }

        // 입력값·이미지는 서버에서도 검증한다.
        saveDto.validate();

        if (file == null || file.isEmpty()) {
            throw new Exception400("대표 이미지를 등록해주세요.");
        }

        // 기존 서비스의 저장·검증·트랜잭션을 그대로 사용한다.
        eventService.save(saveDto, company.getId(), file);

        // 화면 이동은 저장 성공 후 JS에서 수행한다.
        return Map.of("redirectUrl", "/events");
    }
}
package com.busan.eventplatform.main;

import com.busan.eventplatform.event.event_category.EventCategoryResponse;
import com.busan.eventplatform.event.event_category.EventCategoryService;
import com.busan.eventplatform.event.events.EventService;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(Define.SESSION_USER);
        Long userId = (sessionUserDto == null) ? null : sessionUserDto.getId();

        model.addAttribute("eventPage", eventService.findAllPage(1, 12, "", userId));
        EventCategoryResponse.ListDto resDto = eventCategoryService.findAllList();

        model.addAttribute("categoryList", resDto.getCategoryList());
        return "main";
    }
}

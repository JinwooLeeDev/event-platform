package com.busan.eventplatform.main;

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

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("eventPage", eventService.findAllPage(1, 8, ""));
        return "main";
    }
}

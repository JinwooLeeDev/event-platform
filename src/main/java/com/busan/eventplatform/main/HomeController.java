package com.busan.eventplatform.main;

import com.busan.eventplatform.event.event_category.EventCategoryResponse;
import com.busan.eventplatform.event.event_category.EventCategoryService;
import com.busan.eventplatform.event.events.EventService;
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
    public String home(Model model) {
        model.addAttribute("eventPage", eventService.findAllPage(1, 12, ""));
        EventCategoryResponse.ListDto resDto = eventCategoryService.findAllList();

        model.addAttribute("categoryList", resDto.getCategoryList());
        return "main";
    }
}

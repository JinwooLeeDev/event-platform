package com.busan.eventplatform.event.event_category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@RequiredArgsConstructor
@Controller
public class EventCategoryController {

    private final EventCategoryService eventCategoryService;

    // TODO - url 수정하기
    // POST - http://localhost:8080/categories
    @PostMapping("/categories")
    public String save(EventCategoryRequest.SaveDto saveDto) {
        saveDto.validate();
        eventCategoryService.save(saveDto);
        return "redirect:/admin";
    }

    // GET - http://localhost:8080/categories
    @GetMapping("/categories")
    public String findAllList(Model model) {
        EventCategoryResponse.ListDto resDto = eventCategoryService.findAllList();
        model.addAttribute("categoryList", resDto);
        return "categories";
    }

    // POST - http://localhost:8080/categories/update/1
    @PostMapping("/categories/update/{id}")
    public String update(@PathVariable Long id, EventCategoryRequest.UpdateDto updateDto) {
        updateDto.validate();
        eventCategoryService.update(id, updateDto);
        return "redirect:/categories/";
    }

    // POST - http://localhost:8080/categories/delete/1
    @PostMapping("/categories/delete/{id}")
    public String delete(@PathVariable Long id) {
        eventCategoryService.delete(id);
        return "redirect:/categories/";
    }
}

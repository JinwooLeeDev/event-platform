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
    // POST - http://localhost:8080/admin/categories
    @PostMapping("/admin/categories")
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

    // POST - http://localhost:8080/admin/categories/1/update
    @PostMapping("/admin/categories/{id}/update")
    public String update(@PathVariable Long id, EventCategoryRequest.UpdateDto updateDto) {
        updateDto.validate();
        eventCategoryService.update(id, updateDto);
        return "redirect:/admin/categories/";
    }

    // POST - http://localhost:8080/admin/categories/1/delete
    @PostMapping("/admin/categories/{id}/delete")
    public String delete(@PathVariable Long id) {
        eventCategoryService.delete(id);
        return "redirect:/admin/categories/";
    }
}

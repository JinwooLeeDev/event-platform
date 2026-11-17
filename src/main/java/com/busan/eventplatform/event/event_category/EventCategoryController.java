package com.busan.eventplatform.event.event_category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
@Controller
public class EventCategoryController {

    private final EventCategoryService eventCategoryService;

    // TODO - url 수정하기
    // POST - http://localhost:8080/categories
    @PostMapping({"/categories", "/admin/categories"})
    public String save(EventCategoryRequest.SaveDto saveDto) {
        saveDto.validate();
        eventCategoryService.save(saveDto);
        return "redirect:/admin/categories";
    }

    // GET - http://localhost:8080/categories
    @GetMapping({"/categories", "/admin/categories"})
    public String findAllList(Model model) {
        EventCategoryResponse.ListDto resDto = eventCategoryService.findAllList();
        model.addAttribute("categoryList", resDto.getCategoryList());
        return com.busan.eventplatform.admin.AdminPageView.prepare(model, "카테고리 관리", "행사 분류에 사용할 카테고리를 등록·수정·삭제하세요.", "categories");
    }

    // POST - http://localhost:8080/categories/update/1
    @PostMapping({"/categories/update/{id}", "/admin/categories/update/{id}"})
    public String update(@PathVariable Long id, EventCategoryRequest.UpdateDto updateDto) {
        updateDto.validate();
        eventCategoryService.update(id, updateDto);
        return "redirect:/admin/categories";
    }

    // POST - http://localhost:8080/categories/delete/1
    @PostMapping({"/categories/delete/{id}", "/admin/categories/delete/{id}"})
    public String delete(@PathVariable Long id) {
        eventCategoryService.delete(id);
        return "redirect:/admin/categories";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({com.busan.eventplatform.global.error.exception.Exception400.class,
            org.springframework.dao.DataIntegrityViolationException.class})
    public String categoryError(RuntimeException error, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        redirect.addFlashAttribute("categoryError", error instanceof org.springframework.dao.DataIntegrityViolationException
                ? "행사에 사용 중인 카테고리는 삭제할 수 없습니다." : error.getMessage());
        return "redirect:/admin/categories";
    }
}

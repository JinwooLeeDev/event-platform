package com.busan.eventplatform.event.event_category;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventCategoryService {

    private final EventCategoryRepository eventCategoryRepository;

    // 새로운 카테고리 등록 (관리자용)
    @Transactional
    public EventCategory save(EventCategoryRequest.SaveDto saveDto) {
        if (eventCategoryRepository.findByName(saveDto.getName()).isPresent()) {
            throw new Exception400("이미 존재하는 카테고리입니다.");
        }

        return eventCategoryRepository.save(saveDto.toEntity());
    }

    // 전체 카테고리 목록 조회
    public EventCategoryResponse.ListDto findAllList() {
        List<EventCategory> categoryList = eventCategoryRepository.findAll();
        return new EventCategoryResponse.ListDto(categoryList);
    }


    // 기존 카테고리 이름 수정 (관리자용)
    public EventCategory update(Long id, EventCategoryRequest.UpdateDto updateDto) {
        EventCategory eventCategory = eventCategoryRepository.findById(id)
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));

        EventCategory findCategory = eventCategoryRepository.findByName(updateDto.getName())
                .orElse(null);

        if (findCategory != null && !findCategory.getId().equals(id)) {
            throw new Exception400("이미 존재하는 카테고리입니다.");
        }
        eventCategory.setName(updateDto.getName());

        return eventCategory;
    }

    // 기존 카테고리 삭제 (관리자용)
    public void delete(Long id) {
        EventCategory eventCategory = eventCategoryRepository.findById(id)
                .orElseThrow(() -> new Exception404("카테고리를 찾을 수 없습니다."));

        eventCategoryRepository.delete(eventCategory);
    }

}

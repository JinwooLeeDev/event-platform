package com.busan.eventplatform.event.event_category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventCategoryService {

    private EventCategoryRepository repository;

    // 새로운 카테고리 등록 (추후 admin과 연결)
    /**
     * 카테고리 이름 비어 있는지 확인
     * 이미 동일한 이름이 존재하는지 확인
     * 새로운 카테고리 객체 생성
     * Repository를 통해 저장
     */
    public EventCategory save(EventCategoryRequest.)

    // 전체 카테고리 목록 조회
    /**
     * Repository의 findAll 호출
     * 조회한 카테고리 목록 반환
     */

    // 기존 카테고리 이름 수정
    /**
     * 수정할 카테고리 ID로 기존 데이터 조회
     * 새로운 이름이 비어 있는지 확인
     * 다른 카테고리와 이름이 중복되는지 확인
     * 기존 카테고리 이름 변경
     * 변경 사항 저장
     */

    // 카테고리 이름 중복 검증

}

//
//package com.busan.eventplatform.event.events;
//
//import com.busan.eventplatform.global.error.exception.Exception400;
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class EventRequestTest {
//
//    // 정상적인 행사 등록 DTO 생성
//    private EventRequest.SaveDto createSaveDto() {
//        EventRequest.SaveDto dto = new EventRequest.SaveDto();
//        dto.setTitle("부산 전시회");
//        dto.setSummary("전시회 소개");
//        dto.setDescription("전시회 상세 설명");
//        dto.setAddress("부산광역시 해운대구");
//        dto.setCategoryId(1L);
//        dto.setApplyStartAt(LocalDateTime.now());
//        dto.setApplyEndAt(LocalDateTime.now().plusDays(7));
//        return dto;
//    }
//
//    // 정상적인 행사 수정 DTO 생성
//    private EventRequest.UpdateDto createUpdateDto() {
//        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();
//        dto.setTitle("수정된 전시회");
//        dto.setSummary("수정된 소개");
//        dto.setDescription("수정된 설명");
//        dto.setAddress("부산광역시 수영구");
//        dto.setCategoryId(1L);
//        dto.setApplyStartAt(LocalDateTime.now());
//        dto.setApplyEndAt(LocalDateTime.now().plusDays(7));
//        return dto;
//    }
//
//    // 정상적인 행사 등록
//    @Test
//    void saveDto_validate_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//
//        assertDoesNotThrow(dto::validate);
//    }
//
//    // 제목 누락
//    @Test
//    void saveDto_titleNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setTitle(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 소개 누락
//    @Test
//    void saveDto_summaryNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setSummary("");
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 상세 설명 누락
//    @Test
//    void saveDto_descriptionNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setDescription(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 주소 누락
//    @Test
//    void saveDto_addressNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setAddress(" ");
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 카테고리 누락
//    @Test
//    void saveDto_categoryNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setCategoryId(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 모집 시작일 누락
//    @Test
//    void saveDto_applyStartAtNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setApplyStartAt(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 모집 마감일 누락
//    @Test
//    void saveDto_applyEndAtNull_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setApplyEndAt(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 모집 시작일이 마감일보다 늦은 경우
//    @Test
//    void saveDto_invalidDate_test() {
//        EventRequest.SaveDto dto = createSaveDto();
//        dto.setApplyStartAt(LocalDateTime.now().plusDays(10));
//        dto.setApplyEndAt(LocalDateTime.now().plusDays(5));
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 정상적인 행사 수정
//    @Test
//    void updateDto_validate_test() {
//        EventRequest.UpdateDto dto = createUpdateDto();
//
//        assertDoesNotThrow(dto::validate);
//    }
//
//    // 행사 수정 시 제목 누락
//    @Test
//    void updateDto_titleNull_test() {
//        EventRequest.UpdateDto dto = createUpdateDto();
//        dto.setTitle(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 행사 수정 시 카테고리 누락
//    @Test
//    void updateDto_categoryNull_test() {
//        EventRequest.UpdateDto dto = createUpdateDto();
//        dto.setCategoryId(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 행사 수정 시 날짜 오류
//    @Test
//    void updateDto_invalidDate_test() {
//        EventRequest.UpdateDto dto = createUpdateDto();
//        dto.setApplyStartAt(LocalDateTime.now().plusDays(10));
//        dto.setApplyEndAt(LocalDateTime.now().plusDays(5));
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//
//    // 정상적인 취소 사유
//    @Test
//    void cancelDto_validate_test() {
//        EventRequest.CancelDto dto = new EventRequest.CancelDto();
//        dto.setCancellationReason("행사 운영 사정");
//
//        assertDoesNotThrow(dto::validate);
//    }
//
//    // 취소 사유 누락
//    @Test
//    void cancelDto_reasonNull_test() {
//        EventRequest.CancelDto dto = new EventRequest.CancelDto();
//        dto.setCancellationReason(null);
//
//        assertThrows(Exception400.class, dto::validate);
//    }
//}


package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.global.error.exception.Exception400;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventRequestTest {

    private EventRequest.SaveDto createValidSaveDto() {
        EventRequest.SaveDto dto = new EventRequest.SaveDto();

        dto.setTitle("부산 전시 행사");
        dto.setSummary("전시 행사 소개");
        dto.setDescription("전시 행사 상세 설명");
        dto.setAddress("부산광역시 해운대구");
        dto.setCategoryId(10L);

        dto.setHasSessions(false);
        dto.setStartDate(LocalDate.of(2026, 12, 1));
        dto.setEndDate(LocalDate.of(2026, 12, 31));

        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 30, 18, 0));

        dto.setPricingType("FREE");
        dto.setFee(0);

        return dto;
    }

    // =========================
    // 행사 등록 DTO
    // =========================

    @Test
    @DisplayName("올바른 무료 행사 등록 정보는 검증을 통과한다")
    void saveDto_validate_success() {
        EventRequest.SaveDto dto = createValidSaveDto();

        assertDoesNotThrow(dto::validate);
        assertEquals(0, dto.getFee());
    }

    @Test
    @DisplayName("제목이 없으면 예외가 발생한다")
    void saveDto_validate_titleMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setTitle(" ");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("소개가 없으면 예외가 발생한다")
    void saveDto_validate_summaryMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setSummary(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("설명이 없으면 예외가 발생한다")
    void saveDto_validate_descriptionMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setDescription("");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("주소가 없으면 예외가 발생한다")
    void saveDto_validate_addressMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setAddress(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("카테고리가 없으면 예외가 발생한다")
    void saveDto_validate_categoryMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setCategoryId(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 시작일이 없으면 예외가 발생한다")
    void saveDto_validate_startDateMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setStartDate(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 종료일이 시작일보다 빠르면 예외가 발생한다")
    void saveDto_validate_invalidDateRange() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setStartDate(LocalDate.of(2026, 12, 31));
        dto.setEndDate(LocalDate.of(2026, 12, 1));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("모집 시작일이 없으면 예외가 발생한다")
    void saveDto_validate_applyStartMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setApplyStartAt(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("모집 마감일이 시작일보다 빠르면 예외가 발생한다")
    void saveDto_validate_invalidApplyRange() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setApplyStartAt(LocalDateTime.of(2026, 11, 30, 18, 0));
        dto.setApplyEndAt(LocalDateTime.of(2026, 11, 20, 9, 0));

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("무료 행사는 참가비를 0원으로 설정한다")
    void saveDto_validate_freeFee() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setFee(50000);

        dto.validate();

        assertEquals(0, dto.getFee());
    }

    @Test
    @DisplayName("유료 행사에 참가비가 없으면 예외가 발생한다")
    void saveDto_validate_paidFeeMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setPricingType("PAID");
        dto.setFee(0);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("유효하지 않은 참가비 유형이면 예외가 발생한다")
    void saveDto_validate_invalidPricingType() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setPricingType("UNKNOWN");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("회차를 사용하는 행사에 회차 정보가 없으면 예외가 발생한다")
    void saveDto_validate_sessionsMissing() {
        EventRequest.SaveDto dto = createValidSaveDto();
        dto.setHasSessions(true);
        dto.setSessions(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 등록 DTO를 Event 엔티티로 변환한다")
    void saveDto_toEntity() {
        EventRequest.SaveDto dto = createValidSaveDto();

        Event event = dto.toEntity();

        assertEquals("부산 전시 행사", event.getTitle());
        assertEquals("전시 행사 소개", event.getSummary());
        assertEquals("전시 행사 상세 설명", event.getDescription());
        assertEquals("부산광역시 해운대구", event.getAddress());
    }

    // =========================
    // 행사 수정 DTO
    // =========================

    @Test
    @DisplayName("올바른 행사 수정 정보는 검증을 통과한다")
    void updateDto_validate_success() {
        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();

        dto.setTitle("수정된 행사");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 설명");
        dto.setAddress("부산광역시 수영구");
        dto.setCategoryId(10L);

        assertDoesNotThrow(dto::validate);
    }

    @Test
    @DisplayName("행사 수정 시 제목이 없으면 예외가 발생한다")
    void updateDto_validate_titleMissing() {
        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setTitle("");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 수정 시 소개가 없으면 예외가 발생한다")
    void updateDto_validate_summaryMissing() {
        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setSummary(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 수정 시 설명이 없으면 예외가 발생한다")
    void updateDto_validate_descriptionMissing() {
        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setDescription(" ");

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 수정 시 주소가 없으면 예외가 발생한다")
    void updateDto_validate_addressMissing() {
        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setAddress(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 수정 시 카테고리가 없으면 예외가 발생한다")
    void updateDto_validate_categoryMissing() {
        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setCategoryId(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("행사 수정 DTO로 기존 엔티티를 수정한다")
    void updateDto_toEntity() {
        EventCategory category = mock(EventCategory.class);

        Event event = Event.builder()
                .title("기존 행사")
                .summary("기존 소개")
                .description("기존 설명")
                .address("부산광역시 해운대구")
                .build();

        EventRequest.UpdateDto dto = createValidUpdateDto();
        dto.setTitle("수정된 행사");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 설명");
        dto.setAddress("부산광역시 수영구");

        Event result = dto.toEntity(event, category);

        assertSame(event, result);
        assertEquals("수정된 행사", result.getTitle());
        assertEquals("수정된 소개", result.getSummary());
        assertEquals("수정된 설명", result.getDescription());
        assertEquals("부산광역시 수영구", result.getAddress());
        assertSame(category, result.getCategory());
    }

    private EventRequest.UpdateDto createValidUpdateDto() {
        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();

        dto.setTitle("수정된 행사");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 설명");
        dto.setAddress("부산광역시 수영구");
        dto.setCategoryId(10L);

        return dto;
    }

    // =========================
    // 행사 취소 DTO
    // =========================

    @Test
    @DisplayName("올바른 취소 사유는 검증을 통과한다")
    void cancelDto_validate_success() {
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("행사 운영 사정");

        assertDoesNotThrow(dto::validate);
    }

    @Test
    @DisplayName("취소 사유가 없으면 예외가 발생한다")
    void cancelDto_validate_reasonMissing() {
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason(null);

        assertThrows(Exception400.class, dto::validate);
    }

    @Test
    @DisplayName("취소 사유가 공백이면 예외가 발생한다")
    void cancelDto_validate_reasonBlank() {
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("   ");

        assertThrows(Exception400.class, dto::validate);
    }
}

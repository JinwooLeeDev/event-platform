package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EventRequestTest {

    // =========================
    // 행사 등록 SaveDto
    // =========================

    // 1. 정상적인 행사 등록
    @Test
    void saveDto_validate_success_test() {
        // given
        EventRequest.SaveDto dto = new EventRequest.SaveDto();

        dto.setTitle("부산 문화 축제");
        dto.setSummary("부산에서 진행하는 문화 축제");
        dto.setDescription("다양한 문화 체험 행사입니다.");
        dto.setAddress("부산광역시 해운대구");
        dto.setCategoryId(1L);

        // when
        dto.validate();

        // then
        assertThat(dto.getTitle()).isEqualTo("부산 문화 축제");
    }

    // 2. 제목 누락
    @Test
    void saveDto_title_null_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setTitle(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("제목은 필수 입니다.");
    }

    // 3. 제목 공백
    @Test
    void saveDto_title_blank_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setTitle("   ");

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // 4. 소개 누락
    @Test
    void saveDto_summary_null_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setSummary(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("소개는 필수 입니다.");
    }

    // 5. 설명 누락
    @Test
    void saveDto_description_null_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setDescription(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("설명은 필수 입니다.");
    }

    // 6. 주소 누락
    @Test
    void saveDto_address_null_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setAddress(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("주소는 필수 입니다.");
    }

    // 7. 카테고리 누락
    @Test
    void saveDto_category_null_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();
        dto.setCategoryId(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("카테고리는 필수 입니다.");
    }

    // 8. SaveDto를 Event 엔티티로 변환
    @Test
    void saveDto_toEntity_test() {
        // given
        EventRequest.SaveDto dto = createSaveDto();

        // when
        Event event = dto.toEntity();

        // then
        assertThat(event.getTitle()).isEqualTo(dto.getTitle());
        assertThat(event.getSummary()).isEqualTo(dto.getSummary());
        assertThat(event.getDescription()).isEqualTo(dto.getDescription());
        assertThat(event.getAddress()).isEqualTo(dto.getAddress());

        // 서비스에서 별도로 설정하는 값
        assertThat(event.getCompany()).isNull();
        assertThat(event.getCategory()).isNull();
    }

    // =========================
    // 행사 수정 UpdateDto
    // =========================

    // 9. 정상적인 행사 수정
    @Test
    void updateDto_validate_success_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();

        // when
        dto.validate();

        // then
        assertThat(dto.getTitle()).isEqualTo("수정된 부산 축제");
    }

    // 10. 수정 제목 누락
    @Test
    void updateDto_title_null_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();
        dto.setTitle(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("제목은 필수 입니다.");
    }

    // 11. 수정 소개 누락
    @Test
    void updateDto_summary_null_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();
        dto.setSummary(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // 12. 수정 설명 누락
    @Test
    void updateDto_description_null_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();
        dto.setDescription(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // 13. 수정 주소 누락
    @Test
    void updateDto_address_null_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();
        dto.setAddress(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // 14. 수정 카테고리 누락
    @Test
    void updateDto_category_null_test() {
        // given
        EventRequest.UpdateDto dto = createUpdateDto();
        dto.setCategoryId(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // 15. 기존 Event 엔티티 값 수정
    @Test
    void updateDto_toEntity_test() {
        // given
        Event event = Event.builder()
                .title("기존 제목")
                .summary("기존 소개")
                .description("기존 설명")
                .address("기존 주소")
                .build();

        EventCategory category = mock(EventCategory.class);

        EventRequest.UpdateDto dto = createUpdateDto();

        // when
        Event result = dto.toEntity(event, category);

        // then
        assertThat(result.getTitle()).isEqualTo("수정된 부산 축제");
        assertThat(result.getSummary()).isEqualTo("수정된 소개");
        assertThat(result.getDescription()).isEqualTo("수정된 설명");
        assertThat(result.getAddress()).isEqualTo("수정된 주소");
        assertThat(result.getCategory()).isSameAs(category);

        // 기존 객체 자체를 수정했는지 확인
        assertThat(result).isSameAs(event);
    }

    // =========================
    // 행사 취소 CancelDto
    // =========================

    // 16. 정상적인 행사 취소
    @Test
    void cancelDto_validate_success_test() {
        // given
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("주최 측 사정으로 취소합니다.");

        // when
        dto.validate();

        // then
        assertThat(dto.getCancellationReason())
                .isEqualTo("주최 측 사정으로 취소합니다.");
    }

    // 17. 취소 사유 누락
    @Test
    void cancelDto_reason_null_test() {
        // given
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason(null);

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class)
                .hasMessage("취소 사유를 입력해주세요.");
    }

    // 18. 취소 사유 공백
    @Test
    void cancelDto_reason_blank_test() {
        // given
        EventRequest.CancelDto dto = new EventRequest.CancelDto();
        dto.setCancellationReason("   ");

        // when & then
        assertThatThrownBy(dto::validate)
                .isInstanceOf(Exception400.class);
    }

    // =========================
    // 테스트 데이터 생성 메서드
    // =========================

    private EventRequest.SaveDto createSaveDto() {
        EventRequest.SaveDto dto = new EventRequest.SaveDto();

        dto.setTitle("부산 문화 축제");
        dto.setSummary("부산에서 진행하는 문화 축제");
        dto.setDescription("다양한 문화 체험 행사입니다.");
        dto.setAddress("부산광역시 해운대구");
        dto.setCategoryId(1L);

        return dto;
    }

    private EventRequest.UpdateDto createUpdateDto() {
        EventRequest.UpdateDto dto = new EventRequest.UpdateDto();

        dto.setTitle("수정된 부산 축제");
        dto.setSummary("수정된 소개");
        dto.setDescription("수정된 설명");
        dto.setAddress("수정된 주소");
        dto.setCategoryId(2L);

        return dto;
    }
}
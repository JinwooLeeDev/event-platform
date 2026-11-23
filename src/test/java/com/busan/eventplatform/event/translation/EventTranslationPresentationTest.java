package com.busan.eventplatform.event.translation;

import com.busan.eventplatform.event.events.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventTranslationPresentationTest {
    EventTranslationService translations=mock(EventTranslationService.class);
    EventTranslationAfterCommit requests;
    EventTranslationPresentation presentation;
    @BeforeEach void setup(){requests=new EventTranslationAfterCommit(translations);presentation=new EventTranslationPresentation(translations,requests);}
    @AfterEach void reset(){LocaleContextHolder.resetLocaleContext();}
    Event event(){return Event.builder().id(1L).title("원문 제목").summary("원문 요약").description("원문 설명").address("부산 주소")
            .placeName("원문 장소").status(EventStatus.PUBLISHED).build();}
    @Test void completeCacheChangesOnlyDetailContentAndLeavesEntityUntouched(){
        var event=event();var dto=new EventResponse.DetailDto(event,false,2L,1L,false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        when(translations.findCachedTranslations(anyMap(),eq("en"))).thenReturn(Map.of(1L,new TranslatedContent("English title","English summary","English description")));
        presentation.detail(event,dto);
        assertThat(dto.getTitle()).isEqualTo("English title");assertThat(dto.getSummary()).isEqualTo("English summary");
        assertThat(dto.getDescription()).isEqualTo("English description");assertThat(dto.getAddress()).isEqualTo("부산 주소");
        assertThat(dto.getPlaceName()).isEqualTo("원문 장소");assertThat(dto.getConfirmedCount()).isEqualTo(2L);
        assertThat(event.getTitle()).isEqualTo("원문 제목");verify(translations,never()).requestTranslation(anyLong(),anyString());
    }
    @Test void listReadsCacheOnceAndPreservesOrdering(){
        LocaleContextHolder.setLocale(Locale.ENGLISH);var event=event();var dto=new EventResponse.EventDto(event);
        when(translations.findCachedTranslations(anyMap(),eq("en"))).thenReturn(Map.of(1L,new TranslatedContent("English","Summary","Description")));
        presentation.list(List.of(event),List.of(dto));assertThat(dto.getTitle()).isEqualTo("English");
        verify(translations,times(1)).findCachedTranslations(anyMap(),eq("en"));
    }
    @Test void koreanAndUnpublishedEventsKeepOriginalWithoutTranslationQueries(){
        var event=event();var dto=new EventResponse.DetailDto(event,false,0L,0L,false);
        LocaleContextHolder.setLocale(Locale.KOREAN);presentation.detail(event,dto);
        LocaleContextHolder.setLocale(Locale.JAPANESE);event.setStatus(EventStatus.PENDING_REVIEW);presentation.detail(event,dto);
        verifyNoInteractions(translations);assertThat(dto.getTitle()).isEqualTo("원문 제목");
    }
    @Test void cacheQueryFailureKeepsOriginalAndDoesNotFailThePage(){
        LocaleContextHolder.setLocale(Locale.ENGLISH);var event=event();var dto=new EventResponse.DetailDto(event,false,0L,0L,false);
        when(translations.findCachedTranslations(anyMap(),anyString())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatCode(()->presentation.detail(event,dto)).doesNotThrowAnyException();assertThat(dto.getTitle()).isEqualTo("원문 제목");
    }
    @Test void cachedJapaneseDoesNotRegisterAnotherRequest(){
        LocaleContextHolder.setLocale(Locale.JAPANESE);var event=event();var dto=new EventResponse.DetailDto(event,false,0L,0L,false);
        when(translations.findCachedTranslations(anyMap(),eq("ja"))).thenReturn(Map.of(1L,new TranslatedContent("翻訳","概要","説明")));
        presentation.detail(event,dto);assertThat(dto.getTitle()).isEqualTo("翻訳");
        verify(translations,never()).requestTranslation(anyLong(),anyString());
    }
    @Test void lazyRegistrationFailureStillReturnsOriginal(){
        LocaleContextHolder.setLocale(Locale.JAPANESE);var event=event();var dto=new EventResponse.DetailDto(event,false,0L,0L,false);
        when(translations.findCachedTranslations(anyMap(),eq("ja"))).thenReturn(Map.of());
        when(translations.requestTranslation(1,"ja")).thenThrow(new IllegalStateException("unavailable"));
        assertThatCode(()->presentation.detail(event,dto)).doesNotThrowAnyException();assertThat(dto.getTitle()).isEqualTo("원문 제목");
    }
    @ParameterizedTest @ValueSource(strings={"ja","zh-CN"})
    void listMissRegistersLazyWorkButKeepsOriginalFirstResponse(String locale){
        LocaleContextHolder.setLocale(Locale.forLanguageTag(locale));var event=event();var dto=new EventResponse.EventDto(event);
        when(translations.findCachedTranslations(anyMap(),eq(locale))).thenReturn(Map.of());
        presentation.list(List.of(event),List.of(dto));assertThat(dto.getTitle()).isEqualTo("원문 제목");
        verify(translations,times(1)).requestTranslation(1,locale);
    }
}

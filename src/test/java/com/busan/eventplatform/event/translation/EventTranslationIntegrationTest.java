package com.busan.eventplatform.event.translation;

import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.eventCategory.EventCategoryRepository;
import com.busan.eventplatform.event.events.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.Locale;
import java.time.Clock;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("h2")
@SpringBootTest(properties={
        "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:translation_integration;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "gemini.api.key=", "gemini.api.model=",
        "gemini.api.worker-enabled=false", "app.mail.from=audit@example.test", "spring.mail.host=127.0.0.1",
        "spring.mail.username=audit", "spring.mail.password=placeholder", "app.business-verification.service-key=placeholder",
        "portone.naver-store-id=placeholder", "portone.naver-channel-key=placeholder", "portone.naver-secret-key=placeholder",
        "portone.kakao-store-id=placeholder", "portone.kakao-channel-key=placeholder", "portone.kakao-secret-key=placeholder"
})
class EventTranslationIntegrationTest {
    @Autowired EventRepository events;
    @Autowired EventService eventService;
    @Autowired CompanyRepository companies;
    @Autowired EventCategoryRepository categories;
    @Autowired PlatformTransactionManager manager;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate db;
    @Autowired TranslationWorker worker;
    @MockitoBean TranslationClient client;
    @MockitoBean(name="translationClock") Clock clock;
    TransactionTemplate tx;
    long eventId;
    @BeforeEach void setup() throws Exception {
        try(var connection=dataSource.getConnection()){assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");}
        new ResourceDatabasePopulator(new ClassPathResource("db/manual/event-translation.sql")).execute(dataSource);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-21T00:00:00Z"));
        db.update("delete from events"); // This test's isolated, empty memory database only.
        db.update("update translation_budget set minute_count=0,day_count=0,blocked_until=null where id=1");
        tx=new TransactionTemplate(manager);
        eventId=tx.execute(ignored->events.saveAndFlush(Event.builder().company(companies.findAll().getFirst())
                .category(categories.findAll().getFirst()).title("원문 제목").summary("원문 요약").description("원문 설명")
                .address("부산 원문 주소").placeName("원문 장소").status(EventStatus.PAID_READY).build()).getId());
        when(client.translate(any(),any())).thenAnswer(call->switch((String)call.getArgument(1)){
            case "ja" -> new TranslatedContent("釜山のイベント","概要","説明");
            case "zh-CN" -> new TranslatedContent("釜山活动","摘要","说明");
            default -> new TranslatedContent("Busan event","Event summary","Event description");
        });
    }
    @AfterEach void resetLocale(){LocaleContextHolder.resetLocaleContext();}
    long jobs(){return db.queryForObject("select count(*) from translation_job where event_id=?",Long.class,eventId);}
    void publish(){tx.executeWithoutResult(ignored->{var event=events.findById(eventId).orElseThrow();event.setStatus(EventStatus.PUBLISHED);events.flush();assertThat(jobs()).isZero();});}
    @Test void publicationAndChangesQueueOnlyAfterCommitAndSameHashDoesNotTranslateAgain(){
        assertThat(jobs()).isZero();publish();assertThat(jobs()).isEqualTo(1);
        verify(client,never()).translate(any(),any());assertThat(worker.runOne()).isTrue();
        tx.executeWithoutResult(ignored->events.findById(eventId).orElseThrow().setAddress("변경된 원문 주소"));
        assertThat(jobs()).isEqualTo(1);assertThat(worker.runOne()).isFalse();
        tx.executeWithoutResult(ignored->{events.findById(eventId).orElseThrow().setSummary("수정된 원문 요약");events.flush();assertThat(jobs()).isEqualTo(1);});
        assertThat(jobs()).isEqualTo(2);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(eventService.findById(eventId,null,null).getTitle()).isEqualTo("원문 제목"); // Old cache is stale.
        assertThat(worker.runOne()).isTrue();
        assertThat(eventService.findById(eventId,null,null).getTitle()).isEqualTo("Busan event");
        assertThat(eventService.findAllPage(1,8,"").getEventList().getFirst().getTitle()).isEqualTo("Busan event");
        verify(client,times(2)).translate(any(),eq("en"));
        assertThat(events.findById(eventId).orElseThrow().getTitle()).isEqualTo("원문 제목");
    }
    @Test void rollbackDoesNotRegisterEnglishWork(){
        tx.executeWithoutResult(status->{events.findById(eventId).orElseThrow().setStatus(EventStatus.PUBLISHED);events.flush();status.setRollbackOnly();});
        assertThat(jobs()).isZero();assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(EventStatus.PAID_READY);
    }
    @Test void queueFailureDoesNotUndoPublication(){
        when(client.unavailableReason()).thenThrow(new IllegalStateException("unavailable"));
        assertThatCode(this::publish).doesNotThrowAnyException();
        assertThat(events.findById(eventId).orElseThrow().getStatus()).isEqualTo(EventStatus.PUBLISHED);assertThat(jobs()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"ja","zh-CN"})
    void lazyReadReturnsOriginalThenCacheOnNextDetailAndList(String code){
        publish();worker.runOne();LocaleContextHolder.setLocale(Locale.forLanguageTag(code));
        var first=eventService.findById(eventId,null,null);assertThat(first.getTitle()).isEqualTo("원문 제목");assertThat(jobs()).isEqualTo(2);
        worker.runOne();var translated=eventService.findById(eventId,null,null);
        assertThat(translated.getTitle()).as("queue=%s",db.queryForList("select target_locale,status,last_error from translation_job where event_id=?",eventId)).isNotEqualTo("원문 제목");assertThat(translated.getAddress()).isEqualTo("부산 원문 주소");
        assertThat(eventService.findAllPage(1,8,"").getEventList().getFirst().getTitle()).isEqualTo(translated.getTitle());
        assertThat(jobs()).isEqualTo(2);assertThat(worker.runOne()).isFalse();verify(client,times(1)).translate(any(),eq(code));
    }
    @Test void cacheSqlFailureUsesSeparateTransactionAndDoesNotPoisonOriginalQuery(){
        var failingJdbc=mock(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class);
        var isolatedStore=new TranslationStore(failingJdbc,manager,new TranslationProperties(),Clock.systemUTC());
        tx.executeWithoutResult(status->{
            events.findById(eventId).orElseThrow();
            var originalConnection=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(dataSource);
            when(failingJdbc.queryForList(anyString(),anyMap())).thenAnswer(call->{
                assertThat(org.springframework.jdbc.datasource.DataSourceUtils.getConnection(dataSource)).isNotSameAs(originalConnection);
                throw new org.springframework.jdbc.BadSqlGrammarException("cache","select",new java.sql.SQLException("fixture"));
            });
            assertThatThrownBy(()->isolatedStore.findCachedTranslations(java.util.Map.of(eventId,new SourceContent("ko","t","s","d")),"en"))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(status.isRollbackOnly()).isFalse();
            assertThat(db.queryForObject("select count(*) from events where id=?",Long.class,eventId)).isEqualTo(1L);
        });
    }
}

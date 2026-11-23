package com.busan.eventplatform.event.translation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.busan.eventplatform.event.translation.TranslationResult.Status.*;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;

class TranslationQueueTest {
    JdbcTemplate db;
    TranslationStore store;
    TranslationClient client;
    TranslationProperties config;
    EventTranslationService service;
    TranslationWorker worker;
    MutableClock clock;
    DriverManagerDataSource dataSource;
    @TempDir java.nio.file.Path temporaryDirectory;
    static final TranslatedContent OUTPUT = new TranslatedContent("Translated title", "Translated summary", "<p>Original 2026 {name}</p>");
    @BeforeEach void setup() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:translation_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        db = new JdbcTemplate(dataSource);
        db.execute("create table events(id bigint primary key,title varchar(255),summary varchar(255),description varchar(255),status varchar(30))");
        db.update("insert into events values(1,'Original title','Original summary','<p>Original 2026 {name}</p>','PUBLISHED')");
        migrate();
        config = new TranslationProperties();
        clock = new MutableClock();
        store = new TranslationStore(new NamedParameterJdbcTemplate(dataSource), new DataSourceTransactionManager(dataSource), config, clock);
        client = mock(TranslationClient.class);
        when(client.translate(any(),any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return switch (Objects.toString(invocation.getArgument(1), "en")) {
                case "ja" -> new TranslatedContent("釜山のイベント", "釜山の文化", "<p>釜山 2026 {name}</p>");
                case "zh-CN" -> new TranslatedContent("釜山活动", "釜山文化", "<p>釜山 2026 {name}</p>");
                default -> OUTPUT;
            };
        });
        service = new EventTranslationService(store, client); worker = new TranslationWorker(store, client);
    }
    @AfterEach void close() { db.execute("shutdown"); }
    void migrate() { new ResourceDatabasePopulator(new ClassPathResource("db/manual/event-translation.sql")).execute(dataSource); }
    int jobs() { return db.queryForObject("select count(*) from translation_job", Integer.class); }
    String state() { return db.queryForObject("select status from translation_job where event_id=1 order by id desc fetch first 1 rows only",String.class); }
    @Test void requestPersistsQueueWithoutCallingClientThenCachesResultAndKeepsOriginal() {
        var original = db.queryForMap("select * from events where id=1");
        assertThat(service.findTranslation(1,"en").status()).isEqualTo(TranslationResult.Status.NOT_FOUND);
        assertThat(store.evaluate(1,"en",true,false,null).status()).isEqualTo(PENDING);
        assertThat(jobs()).isEqualTo(1); verify(client,never()).translate(any(),any());
        assertThat(worker.runOne()).isTrue();
        assertThat(service.findTranslation(1,"en").content()).isEqualTo(OUTPUT);
        assertThat(service.requestTranslation(1,"en").status()).isEqualTo(COMPLETED);
        assertThat(worker.runOne()).isFalse(); verify(client,times(1)).translate(any(),eq("en"));
        assertThat(db.queryForMap("select * from events where id=1")).isEqualTo(original);
        assertThat(db.queryForObject("select translated_at from event_translation",Object.class)).isNotNull();
    }
    @Test void simultaneousRequestsAcrossSeparateServiceInstancesCreateOneJob() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<Callable<TranslationResult>>();
            for(int i=0;i<16;i++) tasks.add(() -> new EventTranslationService(
                    new TranslationStore(new NamedParameterJdbcTemplate(dataSource),new DataSourceTransactionManager(dataSource),config,clock),client).requestTranslation(1,"ja"));
            for(var future:pool.invokeAll(tasks)) assertThat(future.get().status()).isEqualTo(PENDING);
            assertThat(jobs()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentWorkersReserveOnlyOneCall() throws Exception {
        service.requestTranslation(1,"en");
        var gate = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(client.translate(any(),any())).thenAnswer(invocation -> { gate.countDown(); release.await(5,TimeUnit.SECONDS); return OUTPUT; });
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(worker::runOne); assertThat(gate.await(5,TimeUnit.SECONDS)).isTrue();
            assertThat(new TranslationWorker(store,client).runOne()).isFalse();
            assertThat(service.getTranslationStatus(1,"en").status()).isEqualTo(PROCESSING);
            release.countDown();assertThat(first.get(5,TimeUnit.SECONDS)).isTrue();
            verify(client,times(1)).translate(any(),any());
        } finally { release.countDown();pool.shutdownNow(); }
    }
    @Test void sourceChangeMakesCacheStaleAndCreatesNewHashJob() {
        service.requestTranslation(1,"en");worker.runOne();
        String hash=service.findTranslation(1,"en").sourceHash();
        db.update("update events set title='Changed title' where id=1");
        assertThat(service.hasSourceChanged(1,hash)).isTrue();
        assertThat(service.findTranslation(1,"en").status()).isEqualTo(STALE);
        assertThat(service.findTranslation(1,"en").content().title()).isEqualTo("Changed title");
        assertThat(service.requestTranslation(1,"en").status()).isEqualTo(PENDING);assertThat(jobs()).isEqualTo(2);
    }
    @Test void inFlightOldResultCannotOverwriteNewCache() {
        service.requestTranslation(1,"en");var old=store.claim();
        db.update("update events set title='New title' where id=1");
        service.requestTranslation(1,"en");
        config.setMaxConcurrent(2);var current=store.claim();
        store.complete(current,OUTPUT);store.complete(old,new TranslatedContent("Old translation","Old summary",OUTPUT.description()));
        assertThat(service.findTranslation(1,"en").content()).isEqualTo(OUTPUT);
        assertThat(db.queryForObject("select last_error from translation_job where id=?",String.class,old.id())).isEqualTo("SOURCE_CHANGED");
    }
    @Test void nonPublicSourceIsNeverSentEvenAfterQueueing() {
        db.update("update events set status='PENDING_REVIEW'");
        assertThat(service.requestTranslation(1,"en").error()).isEqualTo(NOT_PUBLIC);assertThat(jobs()).isZero();
        db.update("update events set status='PUBLISHED'");service.requestTranslation(1,"en");
        db.update("update events set status='CANCELLED'");worker.runOne();
        verify(client,never()).translate(any(),any());assertThat(state()).isEqualTo("FAILED");
    }
    @Test void missingKeyDoesNotQueueAndKoreanAlwaysReturnsOriginal() {
        when(client.unavailableReason()).thenReturn(KEY_MISSING);
        assertThat(service.requestTranslation(1,"en").error()).isEqualTo(KEY_MISSING);
        assertThat(service.requestTranslation(1,"ko").status()).isEqualTo(ORIGINAL);
        assertThat(worker.runOne()).isFalse();assertThat(jobs()).isZero();
    }
    @ParameterizedTest @EnumSource(value=TranslationFailure.Code.class,names={"AUTHENTICATION","MODEL_INVALID","INVALID_RESPONSE","CONTENT_INVALID"})
    void permanentErrorsRemainFailedWithoutAutomaticRetry(TranslationFailure.Code code) {
        service.requestTranslation(1,"en");when(client.translate(any(),any())).thenThrow(new TranslationFailure(code));
        worker.runOne();clock.advance(Duration.ofDays(1));assertThat(worker.runOne()).isFalse();
        assertThat(service.getTranslationStatus(1,"en").error()).isEqualTo(code);
        service.requestTranslation(1,"en");assertThat(state()).isEqualTo("FAILED");
    }
    @ParameterizedTest @EnumSource(value=TranslationFailure.Code.class,names={"RATE_LIMIT","TIMEOUT","TEMPORARY"})
    void transientErrorsBackOffAndEventuallyStop(TranslationFailure.Code code) {
        service.requestTranslation(1,"en");when(client.translate(any(),any())).thenThrow(new TranslationFailure(code));
        worker.runOne();assertThat(state()).isEqualTo("PENDING");assertThat(worker.runOne()).isFalse();
        clock.advance(Duration.ofSeconds(31));worker.runOne();assertThat(state()).isEqualTo("PENDING");
        clock.advance(Duration.ofSeconds(61));worker.runOne();assertThat(state()).isEqualTo("FAILED");
        clock.advance(Duration.ofDays(1));assertThat(worker.runOne()).isFalse();
        assertThat(service.retryTranslation(1,"en").status()).isEqualTo(FAILED);
        verify(client,times(3)).translate(any(),any());
    }
    @Test void retryAfterAlsoPausesOtherJobs() {
        service.requestTranslation(1,"en");service.requestTranslation(1,"ja");
        when(client.translate(any(),any())).thenThrow(new TranslationFailure(RATE_LIMIT,Duration.ofMinutes(10)));
        worker.runOne();clock.advance(Duration.ofMinutes(9));assertThat(worker.runOne()).isFalse();
        clock.advance(Duration.ofMinutes(1));assertThat(worker.runOne()).isTrue();
    }
    @Test void expiredProcessingRecoversAfterSimulatedRestartAndOldTokenCannotWrite() {
        service.requestTranslation(1,"en");var abandoned=store.claim();
        clock.advance(Duration.ofMinutes(3));
        var restarted=new TranslationStore(new NamedParameterJdbcTemplate(dataSource),new DataSourceTransactionManager(dataSource),config,clock);
        assertThat(restarted.claim()).isNull();assertThat(state()).isEqualTo("PENDING");
        clock.advance(Duration.ofSeconds(31));var replacement=restarted.claim();assertThat(replacement).isNotNull();
        restarted.complete(abandoned,new TranslatedContent("Stale","Stale",OUTPUT.description()));
        assertThat(db.queryForObject("select count(*) from event_translation",Integer.class)).isZero();
        restarted.complete(replacement,OUTPUT);assertThat(state()).isEqualTo("COMPLETED");
    }
    @Test void repeatedCrashesAlsoHaveBoundedRetries() {
        config.setMaxRetries(0);service.requestTranslation(1,"en");store.claim();
        clock.advance(Duration.ofMinutes(3));assertThat(store.claim()).isNull();assertThat(state()).isEqualTo("FAILED");
    }
    @Test void budgetPersistsAcrossWorkerRecreationAndResetsAtWindows() {
        config.setRequestsPerMinute(1);config.setRequestsPerDay(2);
        service.requestTranslation(1,"en");service.requestTranslation(1,"ja");service.requestTranslation(1,"zh-CN");
        worker.runOne();assertThat(worker.runOne()).isFalse();clock.advance(Duration.ofMinutes(1));
        assertThat(new TranslationWorker(store,client).runOne()).isTrue();clock.advance(Duration.ofHours(1));
        assertThat(worker.runOne()).isFalse();clock.advance(Duration.ofDays(1));assertThat(worker.runOne()).isTrue();
    }
    @Test void invalidationFencesInFlightResultAndAllowsExplicitBoundedRetry() {
        service.requestTranslation(1,"en");var claim=store.claim();service.invalidateTranslations(1);
        store.complete(claim,OUTPUT);assertThat(service.findTranslation(1,"en").status()).isEqualTo(FAILED);
        assertThat(service.retryTranslation(1,"en").status()).isEqualTo(PENDING);
        worker.runOne();assertThat(service.findTranslation(1,"en").status()).isEqualTo(COMPLETED);
    }
    @Test void invalidLocaleAndTooLongSourceCannotCreateJobs() {
        assertThatThrownBy(()->service.requestTranslation(1,"fr")).isInstanceOf(IllegalArgumentException.class);
        config.setMaxSourceChars(1);assertThat(service.requestTranslation(1,"en").error()).isEqualTo(CONTENT_INVALID);
        assertThat(jobs()).isZero();
    }
    @Test void deletingEventCleansBothTablesAndLateWorkerDoesNothing() {
        service.requestTranslation(1,"en");worker.runOne();service.requestTranslation(1,"ja");var claim=store.claim();
        db.update("delete from events where id=1");store.complete(claim,OUTPUT);
        assertThat(jobs()).isZero();assertThat(db.queryForObject("select count(*) from event_translation",Integer.class)).isZero();
    }
    @Test void schemaIsIdempotentAndMissingSchemaReturnsClearFailure() {
        migrate();service.requestTranslation(1,"en");migrate();assertThat(jobs()).isEqualTo(1);
        db.execute("drop table event_translation");
        assertThat(service.requestTranslation(1,"en").error()).isEqualTo(SCHEMA_NOT_READY);
    }
    @Test void missingAndInvalidLegacyContentReturnFailureInsteadOfLeakingExceptions() {
        assertThat(service.findTranslation(99,"en").error()).isEqualTo(TranslationFailure.Code.NOT_FOUND);
        db.update("update events set summary=null where id=1");
        assertThat(service.requestTranslation(1,"en").error()).isEqualTo(CONTENT_INVALID);
        assertThat(jobs()).isZero();
    }
    @Test void freshWorkerCannotProcessDifferentJobWhileConcurrencySlotIsHeld() {
        service.requestTranslation(1,"en");service.requestTranslation(1,"ja");var first=store.claim();
        assertThat(first).isNotNull();assertThat(store.claim()).isNull();
        store.complete(first,OUTPUT);assertThat(store.claim()).isNotNull();
    }
    @Test void sourceChangedBeforeApiCallFailsWithoutSendingObsoleteContent() {
        service.requestTranslation(1,"en");db.update("update events set title='New title' where id=1");
        worker.runOne();verify(client,never()).translate(any(),any());
        assertThat(db.queryForObject("select last_error from translation_job",String.class)).isEqualTo("SOURCE_CHANGED");
    }
    @Test void fileDatabaseSurvivesEngineShutdownAndNewWorkerRecoversProcessingJob() {
        String url="jdbc:h2:file:"+temporaryDirectory.resolve("restart").toString().replace('\\','/');
        var persistent=new DriverManagerDataSource(url,"sa","");var sql=new JdbcTemplate(persistent);
        sql.execute("create table events(id bigint primary key,title varchar(255),summary varchar(255),description varchar(255),status varchar(30))");
        sql.update("insert into events values(1,'Original title','Original summary','<p>Original 2026 {name}</p>','PUBLISHED')");
        new ResourceDatabasePopulator(new ClassPathResource("db/manual/event-translation.sql")).execute(persistent);
        var first=new TranslationStore(new NamedParameterJdbcTemplate(persistent),new DataSourceTransactionManager(persistent),config,clock);
        new EventTranslationService(first,client).requestTranslation(1,"en");assertThat(first.claim()).isNotNull();
        sql.execute("shutdown"); // Actually close the file DB engine, not just replace a service object.
        clock.advance(Duration.ofMinutes(3));
        var restarted=new TranslationStore(new NamedParameterJdbcTemplate(persistent),new DataSourceTransactionManager(persistent),config,clock);
        try {
            assertThat(restarted.claim()).isNull();clock.advance(Duration.ofSeconds(31));
            assertThat(new TranslationWorker(restarted,client).runOne()).isTrue();
            assertThat(new EventTranslationService(restarted,client).findTranslation(1,"en").status()).isEqualTo(COMPLETED);
            assertThat(sql.queryForObject("select day_count from translation_budget",Integer.class)).isEqualTo(2);
        } finally {sql.execute("shutdown");}
    }
    @Test void revertingToPreviouslyTranslatedSourceRequeuesHistoricalCompletedJobWhenCacheWasReplaced() {
        service.requestTranslation(1,"en");worker.runOne();
        String originalHash=service.findTranslation(1,"en").sourceHash();
        db.update("update events set title='Version B' where id=1");
        service.requestTranslation(1,"en");worker.runOne();
        db.update("update events set title='Original title' where id=1");
        assertThat(service.findTranslation(1,"en").status()).isEqualTo(STALE);
        assertThat(service.requestTranslation(1,"en").status()).isEqualTo(PENDING);
        assertThat(service.requestTranslation(1,"en").status()).isEqualTo(PENDING);
        assertThat(jobs()).isEqualTo(2);assertThat(worker.runOne()).isTrue();
        assertThat(service.findTranslation(1,"en").status()).isEqualTo(COMPLETED);
        assertThat(service.findTranslation(1,"en").sourceHash()).isEqualTo(originalHash);
        verify(client,times(3)).translate(any(),any());
    }
    static final class MutableClock extends Clock {
        Instant instant=Instant.parse("2026-11-21T00:00:00Z");
        void advance(Duration duration) { instant=instant.plus(duration); }
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return instant;}
    }
}

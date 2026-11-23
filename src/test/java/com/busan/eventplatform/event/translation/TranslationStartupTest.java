package com.busan.eventplatform.event.translation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("h2")
@SpringBootTest(properties={
        "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:translation_startup;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "gemini.api.key=", "gemini.api.model=",
        "gemini.api.worker-enabled=false", "app.mail.from=audit@example.test", "spring.mail.host=127.0.0.1",
        "spring.mail.username=audit", "spring.mail.password=placeholder", "app.business-verification.service-key=placeholder",
        "portone.naver-store-id=placeholder", "portone.naver-channel-key=placeholder", "portone.naver-secret-key=placeholder",
        "portone.kakao-store-id=placeholder", "portone.kakao-channel-key=placeholder", "portone.kakao-secret-key=placeholder"
})
class TranslationStartupTest {
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate db;
    @Autowired TranslationClient client;
    @Test void fullApplicationStartsWithoutCredentialsMigrationOrActiveWorker() {
        assertThat(client.unavailableReason()).isEqualTo(TranslationFailure.Code.KEY_MISSING);
        assertThat(context.getBeansOfType(TranslationScheduler.class)).isEmpty();
        assertThat(db.queryForObject("select count(*) from information_schema.tables where lower(table_name) in ('event_translation','translation_job','translation_budget')",Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from events",Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from users",Integer.class)).isPositive();
    }
}

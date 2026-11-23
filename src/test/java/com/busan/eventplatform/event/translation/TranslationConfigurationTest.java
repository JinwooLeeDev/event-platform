package com.busan.eventplatform.event.translation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class TranslationConfigurationTest {
    @Test void noApiKeyOrModelDoesNotPreventStartup() {
        new ApplicationContextRunner().withUserConfiguration(TranslationConfiguration.class)
                .withPropertyValues("gemini.api.key=", "gemini.api.model=").run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(TranslationClient.class);
                    assertThat(context.getBean(TranslationClient.class).unavailableReason()).isEqualTo(TranslationFailure.Code.KEY_MISSING);
                });
    }
    @Test void leaseMustExceedRequestDeadlineAndShutdownGrace() {
        var properties=new TranslationProperties();properties.validate();
        properties.setLease(java.time.Duration.ofSeconds(10));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
    }
}

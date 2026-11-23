package com.busan.eventplatform.event.translation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TranslationProperties.class)
public class TranslationConfiguration {
    @Bean("translationClock") Clock translationClock() { return Clock.systemUTC(); }
    @Bean TranslationClient translationClient(TranslationProperties properties) {
        properties.validate();
        return new GeminiTranslationClient(properties, HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout()).followRedirects(HttpClient.Redirect.NEVER).build());
    }
}

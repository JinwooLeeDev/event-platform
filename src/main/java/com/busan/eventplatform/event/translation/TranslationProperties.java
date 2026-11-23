package com.busan.eventplatform.event.translation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Getter @Setter
@ConfigurationProperties("gemini.api")
public class TranslationProperties {
    private String key = "";
    private String model = "";
    private Duration timeout = Duration.ofSeconds(30);
    private Duration lease = Duration.ofMinutes(2);
    private Duration backoff = Duration.ofSeconds(30);
    private int maxRetries = 2;
    private int maxConcurrent = 1;
    private int requestsPerMinute = 5;
    private int requestsPerDay = 100;
    private int maxSourceChars = 10000;
    private int maxOutputChars = 40000;
    // No toString: this bean contains a credential.
    public void validate() {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(Duration.ofMinutes(2)) > 0
                || lease == null || lease.compareTo(timeout.plusSeconds(30)) < 0 || lease.compareTo(Duration.ofHours(1)) > 0
                || backoff == null || backoff.isNegative() || backoff.isZero() || backoff.compareTo(Duration.ofDays(1)) > 0
                || maxRetries < 0 || maxRetries > 10 || maxConcurrent < 1 || maxConcurrent > 10
                || requestsPerMinute < 1 || requestsPerDay < 1 || maxSourceChars < 1 || maxSourceChars > 100000
                || maxOutputChars < 1 || maxOutputChars > 200000)
            throw new IllegalArgumentException("Invalid translation limits/timeouts");
    }
}

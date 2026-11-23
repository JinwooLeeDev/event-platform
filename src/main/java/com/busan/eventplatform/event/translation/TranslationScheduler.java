package com.busan.eventplatform.event.translation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
@ConditionalOnProperty(name="gemini.api.worker-enabled", havingValue="true")
public class TranslationScheduler {
    private final TranslationWorker worker;
    private final ExecutorService executor;
    private final Semaphore permits;
    private final int concurrency;
    public TranslationScheduler(TranslationWorker worker, TranslationProperties properties) {
        this.worker = worker;
        this.concurrency = properties.getMaxConcurrent();
        this.permits = new Semaphore(concurrency);
        this.executor = Executors.newFixedThreadPool(concurrency, Thread.ofPlatform().name("event-translation-", 0).daemon(true).factory());
    }
    @Scheduled(fixedDelayString="${gemini.api.poll-interval:5000}")
    public void poll() {
        // Keep the existing community/session scheduler thread free of HTTP and queue work.
        for (int i = 0; i < concurrency && permits.tryAcquire(); i++) {
            try { executor.submit(() -> {
                try { worker.runOne(); }
                catch (RuntimeException unavailable) {
                    org.slf4j.LoggerFactory.getLogger(TranslationScheduler.class)
                            .warn("Translation worker unavailable; expired leases will be recovered");
                } finally { permits.release(); }
            }); } catch (RejectedExecutionException stopping) { permits.release(); }
        }
    }
    @PreDestroy public void close() { executor.shutdownNow(); }
}

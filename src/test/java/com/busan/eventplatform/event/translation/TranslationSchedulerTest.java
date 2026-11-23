package com.busan.eventplatform.event.translation;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TranslationSchedulerTest {
    @Test void pollDispatchesOffCallerThreadWithBoundedConcurrency() throws Exception {
        var worker=mock(TranslationWorker.class);var config=new TranslationProperties();config.setMaxConcurrent(2);
        var started=new CountDownLatch(2);var release=new CountDownLatch(1);
        Thread caller=Thread.currentThread();
        when(worker.runOne()).thenAnswer(invocation->{
            assertThat(Thread.currentThread()).isNotSameAs(caller);started.countDown();release.await(5,TimeUnit.SECONDS);return true;
        });
        var scheduler=new TranslationScheduler(worker,config);
        try {
            scheduler.poll();assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();scheduler.poll();
            verify(worker,times(2)).runOne();
        } finally {release.countDown();scheduler.close();}
    }
}

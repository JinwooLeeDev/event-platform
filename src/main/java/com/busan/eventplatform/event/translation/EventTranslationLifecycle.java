package com.busan.eventplatform.event.translation;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Observe all JPA publication/content update paths without changing deposit or event decisions. */
@Component
public class EventTranslationLifecycle {
    private final ApplicationEventPublisher publisher;
    // Standalone Hibernate tools without Spring can still load the entity.
    public EventTranslationLifecycle() { this.publisher = null; }
    @Autowired public EventTranslationLifecycle(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    @PostPersist @PostUpdate
    public void saved(Event event) {
        if (publisher == null || event.getId() == null || event.getStatus() != EventStatus.PUBLISHED) return;
        try { publisher.publishEvent(new EventTranslationRequested(event.getId(), "en")); }
        catch (RuntimeException ignored) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Event translation notification unavailable");
        }
    }
}

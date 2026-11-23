package com.busan.eventplatform.event.translation;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventResponse;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.i18n.SupportedLocale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.*;

/** Presentation-only overlays. Never modify a managed Event or an edit-form DTO. */
@Component
public class EventTranslationPresentation {
    private final EventTranslationService translations;
    private final EventTranslationAfterCommit requests;
    public EventTranslationPresentation(EventTranslationService translations, EventTranslationAfterCommit requests) {
        this.translations = translations; this.requests = requests;
    }
    public void detail(Event original, EventResponse.DetailDto view) {
        var cached = cached(List.of(original));
        var content = cached.get(original.getId());
        if (content != null) {
            view.setTitle(content.title());view.setSummary(content.summary());view.setDescription(content.description());
        }
    }
    public void list(List<Event> originals, List<EventResponse.EventDto> views) {
        var cached = cached(originals);
        for (var view : views) {
            var content = cached.get(view.getId());
            if (content != null) { view.setTitle(content.title());view.setSummary(content.summary()); }
        }
    }
    private Map<Long, TranslatedContent> cached(List<Event> originals) {
        String locale = SupportedLocale.normalize(LocaleContextHolder.getLocale().toLanguageTag());
        if (locale.equals("ko") || originals.isEmpty()) return Map.of();
        try {
            var sources = new LinkedHashMap<Long, SourceContent>();
            for (var event : originals) if (event.getStatus() == EventStatus.PUBLISHED)
                sources.put(event.getId(), new SourceContent("ko", event.getTitle(), event.getSummary(), event.getDescription()));
            if (sources.isEmpty()) return Map.of();
            var cached = translations.findCachedTranslations(sources, locale);
            if (locale.equals("ja") || locale.equals("zh-CN"))
                for (Long id : sources.keySet()) if (!cached.containsKey(id)) requests.schedule(id, locale);
            return cached;
        } catch (RuntimeException ignored) {
            // Missing migration, unavailable DB, malformed legacy content: original page remains usable.
            return Map.of();
        }
    }
}

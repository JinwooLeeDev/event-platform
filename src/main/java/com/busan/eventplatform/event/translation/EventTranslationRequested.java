package com.busan.eventplatform.event.translation;

/** Identifier only: do not retain source text in application events. */
public record EventTranslationRequested(long eventId, String locale) {}

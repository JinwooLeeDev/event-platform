package com.busan.eventplatform.global.i18n;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import java.util.Map;

/** Supplies the same common view context as MVC when testing a template without DispatcherServlet. */
public final class I18nTestModels {
    private static final LocaleMessages MESSAGES = new LocaleMessages(new I18nConfig().messageSource());
    private I18nTestModels() { }

    public static Map<String, Object> withLocale(Object original) {
        var model = new ExtendedModelMap();
        if (original instanceof Map<?, ?> values) {
            values.forEach((key, value) -> model.put(String.valueOf(key), value));
        } else {
            var bean = new BeanWrapperImpl(original);
            for (var property : bean.getPropertyDescriptors()) {
                if (!property.getName().equals("class") && bean.isReadableProperty(property.getName()))
                    model.put(property.getName(), bean.getPropertyValue(property.getName()));
            }
        }
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(LocaleCookieResolver.COOKIE_NAME, "ko"));
        new LocaleViewAdvice(MESSAGES, new LocaleCookieResolver()).addLocale(request, model);
        // These tests assert visible HTML, not JS payload text. Serialization is covered separately and in browser tests.
        model.put("i18nJson", "");
        return model;
    }
}

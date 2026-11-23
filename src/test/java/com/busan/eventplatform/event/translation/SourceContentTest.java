package com.busan.eventplatform.event.translation;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SourceContentTest {
    @Test void hashIsDeterministicAndPreservesBoundariesAndWhitespace() {
        var a = new SourceContent("ko", "ab", "c", "description");
        assertThat(a.hash()).hasSize(64).isEqualTo(new SourceContent("ko", "ab", "c", "description").hash());
        assertThat(a.hash()).isNotEqualTo(new SourceContent("ko", "a", "bc", "description").hash());
        assertThat(a.hash()).isNotEqualTo(new SourceContent("ko", "ab ", "c", "description").hash());
        assertThatThrownBy(() -> new SourceContent("ko", null, "s", "d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TranslationLocale.require("fr")).isInstanceOf(IllegalArgumentException.class);
        assertThat(TranslationLocale.require("zh-CN")).isEqualTo("zh-CN");
    }
}

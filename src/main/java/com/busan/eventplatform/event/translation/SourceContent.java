package com.busan.eventplatform.event.translation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record SourceContent(String sourceLocale, String title, String summary, String description) {
    public SourceContent {
        if (sourceLocale == null || sourceLocale.isBlank() || title == null || title.isBlank()
                || summary == null || summary.isBlank() || description == null || description.isBlank())
            throw new IllegalArgumentException("Nonblank event content required");
    }
    public String hash() {
        try {
            var hash = MessageDigest.getInstance("SHA-256");
            for (String field : List.of("event-content-v1", sourceLocale, title, summary, description)) {
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
                hash.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                hash.update(bytes);
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public int length() { return title.length() + summary.length() + description.length(); }
}

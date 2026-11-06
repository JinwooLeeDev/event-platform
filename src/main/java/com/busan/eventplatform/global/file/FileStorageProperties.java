package com.busan.eventplatform.global.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    private Path uploadDir = Path.of("./uploads");

    private long maxSize = 10L * 1024 * 1024;
}
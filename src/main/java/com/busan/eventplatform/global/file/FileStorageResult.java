package com.busan.eventplatform.global.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileStorageResult {

    private final String storageKey;
    private final String originalFilename;
    private final long size;
    private final String contentType;
}
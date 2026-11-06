package com.busan.eventplatform.global.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileStorageResult {

    private final String storageKey;        // DB에 저장할 키 (파일 구분 키)
    private final String originalFilename;  // 원본 파일 이름
    private final long size;                // 파일 크기 (바이트 단위)
    private final String contentType;       // 파일 유형
}
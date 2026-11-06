package com.busan.eventplatform.global.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileValidationResult {

    private final String originalFilename;  // 파일 원본 이름
    private final String extension;         // 확장자명
    private final String contentType;       // 컨텐츠 타입
}
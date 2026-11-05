package com.busan.eventplatform.global.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    FileStorageResult upload(MultipartFile file);

    Resource load(String storageKey);

    void delete(String storageKey);
}

/*
|  메서드 |       역할       |
| upload() | 로컬 폴더에 저장 |
| load() | 저장 키로 파일 읽기 |
| delete() | 저장 키로 삭제 |
 */
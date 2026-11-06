package com.busan.eventplatform.global.file;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception500;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {

    private final FileStorageProperties properties;

    public FileValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new Exception400("파일을 첨부해주세요.");
        }

        if (file.getSize() > properties.getMaxSize()) {
            throw new Exception400("파일 크기는 10MB 이하여야 합니다.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new Exception400("파일명을 확인해주세요.");
        }

        // 일부 브라우저가 전달하는 경로는 제거하고 이름만 보관
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        if (filename.isBlank() || filename.length() > 255) {
            throw new Exception400("파일명이 비어 있거나 너무 깁니다.");
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex <= 0) {
            throw new Exception400("파일 확장자를 확인해주세요.");
        }

        String extension = filename.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new Exception400(
                    "PDF, JPG, JPEG, PNG 파일만 업로드할 수 있습니다."
            );
        };

        byte[] header;

        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(8);
        } catch (IOException exception) {
            log.error("업로드 파일 읽기 실패", exception);

            throw new Exception500("첨부 파일을 읽지 못했습니다.");
        }

        if (!matchesSignature(extension, header)) {
            throw new Exception400("파일 확장자와 내용이 일치하지 않습니다.");
        }

        return FileValidationResult.builder()
                .originalFilename(filename)
                .extension(extension)
                .contentType(contentType)
                .build();
    }

    private boolean matchesSignature(String extension, byte[] header) {
        return switch (extension) {
            // %PDF-
            case "pdf" -> startsWith(
                    header,
                    new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}
            );

            case "jpg", "jpeg" -> startsWith(
                    header,
                    new byte[]{
                            (byte) 0xFF,
                            (byte) 0xD8,
                            (byte) 0xFF
                    }
            );

            case "png" -> startsWith(
                    header,
                    new byte[]{
                            (byte) 0x89, 0x50, 0x4E, 0x47,
                            0x0D, 0x0A, 0x1A, 0x0A
                    }
            );

            default -> false;
        };
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) {
                return false;
            }
        }

        return true;
    }
}
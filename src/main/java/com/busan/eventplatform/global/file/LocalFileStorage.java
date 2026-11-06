package com.busan.eventplatform.global.file;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.error.exception.Exception500;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile(
                    "^files/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}"
                            + "-[0-9a-f]{4}-[0-9a-f]{12}"
                            + "\\.(pdf|jpg|jpeg|png)$"
            );

    private final FileValidator fileValidator;
    private final FileStorageProperties properties;
    private final Path root;

    public LocalFileStorage(FileValidator fileValidator, FileStorageProperties properties) {
        this.fileValidator = fileValidator;
        this.properties = properties;

        if (properties.getMaxSize() <= 0) {
            throw new IllegalStateException(
                    "업로드 최대 크기는 양수여야 합니다."
            );
        }

        try {
            Path configuredRoot = properties.getUploadDir()
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(configuredRoot);
            this.root = configuredRoot.toRealPath();

            Path filesDirectory = root.resolve("files");

            // 저장 폴더가 다른 위치를 가리키는 링크인지 확인
            if (Files.isSymbolicLink(filesDirectory)) {
                throw new IllegalStateException(
                        "업로드 하위 폴더에 심볼릭 링크를 사용할 수 없습니다."
                );
            }

            Files.createDirectories(filesDirectory);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "파일 저장 폴더를 준비하지 못했습니다.",
                    exception
            );
        }
    }

    @Override
    public FileStorageResult upload(MultipartFile file) {
        FileValidationResult validated =
                fileValidator.validate(file);

        String storageKey = "files/" + UUID.randomUUID() + "." + validated.getExtension();

        Path target = resolvePath(storageKey);

        boolean created = false;
        long size = 0;

        try (
                InputStream input = file.getInputStream();
                OutputStream output = Files.newOutputStream(
                        target,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )
        ) {
            created = true;

            byte[] buffer = new byte[8192];
            int read;

            while ((read = input.read(buffer)) != -1) {
                size += read;

                // 전달된 크기뿐 아니라 실제 읽은 크기도 제한
                if (size > properties.getMaxSize()) {
                    throw new Exception400(
                            "허용된 파일 크기를 초과했습니다."
                    );
                }

                output.write(buffer, 0, read);
            }

        } catch (IOException exception) {
            if (created) {
                cleanup(target);
            }

            log.error("로컬 파일 저장 실패", exception);

            throw new Exception500(
                    "파일을 저장하지 못했습니다."
            );

        } catch (RuntimeException exception) {
            if (created) {
                cleanup(target);
            }

            throw exception;
        }

        return FileStorageResult.builder()
                .storageKey(storageKey)
                .originalFilename(validated.getOriginalFilename())
                .size(size)
                .contentType(validated.getContentType())
                .build();
    }

    @Override
    public Resource load(String storageKey) {
        Path target = resolvePath(storageKey);

        if (!Files.isRegularFile(
                target,
                LinkOption.NOFOLLOW_LINKS
        ) || !Files.isReadable(target)) {
            throw new Exception404(
                    "파일을 찾을 수 없습니다."
            );
        }

        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolvePath(storageKey);

        try {
            // 없는 파일은 이미 삭제된 것으로 처리
            Files.deleteIfExists(target);

        } catch (IOException exception) {
            log.error("로컬 파일 삭제 실패", exception);

            throw new Exception500(
                    "파일을 삭제하지 못했습니다."
            );
        }
    }

    private Path resolvePath(String storageKey) {
        if (storageKey == null
                || !STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new Exception400(
                    "올바르지 않은 파일 저장 키입니다."
            );
        }

        Path target = root.resolve(storageKey).normalize();

        if (!target.startsWith(root)
                || Files.isSymbolicLink(target.getParent())
                || Files.isSymbolicLink(target)) {
            throw new Exception400(
                    "허용되지 않은 파일 경로입니다."
            );
        }

        return target;
    }

    private void cleanup(Path target) {
        try {
            Files.deleteIfExists(target);

        } catch (IOException exception) {
            log.error(
                    "실패한 업로드의 임시 파일 정리 실패: {}",
                    target,
                    exception
            );
        }
    }
}
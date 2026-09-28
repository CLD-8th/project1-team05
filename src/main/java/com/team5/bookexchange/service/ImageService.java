package com.team5.bookexchange.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final Path UPLOAD_DIR =
            Paths.get("uploads", "books");

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    public String save(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return null;
        }

        if (!ALLOWED_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException(
                    "PNG, JPG, JPEG, WebP 이미지만 등록할 수 있습니다."
            );
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "이미지는 최대 5MB까지 등록할 수 있습니다."
            );
        }

        try {
            Files.createDirectories(UPLOAD_DIR);

            String originalName = image.getOriginalFilename();
            String extension = getExtension(originalName);

            String fileName =
                    UUID.randomUUID() + extension;

            Path filePath = UPLOAD_DIR.resolve(fileName);

            image.transferTo(filePath.toAbsolutePath());

            return fileName;

        } catch (IOException e) {
            throw new RuntimeException(
                    "이미지 저장에 실패했습니다.", e
            );
        }
    }

    private String getExtension(String fileName) {

        if (fileName == null ||
                !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        ).toLowerCase();
    }
}
package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResult> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ApiResponse<>(-1, "file empty", null);
        }
        if (file.getSize() > 20 * 1024 * 1024L) {
            return new ApiResponse<>(-1, "file too large", null);
        }

        String uploadsDir = "uploads";
        File dir = new File(uploadsDir);
        if (!dir.exists()) dir.mkdirs();

        String original = sanitizeFilename(file.getOriginalFilename());
        String suffix = "";
        if (original != null && original.contains(".")) {
            suffix = original.substring(original.lastIndexOf('.'));
        }
        String filename = Instant.now().toEpochMilli() + "_" + Math.abs(original == null ? filenameHash() : original.hashCode()) + suffix;

        Path target = Path.of(uploadsDir, filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String url = base + "/uploads/" + filename;

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        String kind = isImage(contentType, suffix) ? "image" : "file";

        return new ApiResponse<>(0, "OK", new UploadResult(
                original == null || original.isBlank() ? filename : original,
                url,
                contentType,
                file.getSize(),
                kind
        ));
    }

    private int filenameHash() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    private boolean isImage(String contentType, String suffix) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String normalized = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        return normalized.equals(".png")
                || normalized.equals(".jpg")
                || normalized.equals(".jpeg")
                || normalized.equals(".gif")
                || normalized.equals(".webp")
                || normalized.equals(".bmp")
                || normalized.equals(".svg");
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return null;
        }
        return original.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    public record UploadResult(String name, String url, String contentType, long size, String kind) {
    }
}

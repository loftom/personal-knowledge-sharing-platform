package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
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

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ApiResponse<>(-1, "file empty", null);
        }

        String uploadsDir = "uploads";
        File dir = new File(uploadsDir);
        if (!dir.exists()) dir.mkdirs();

        String original = file.getOriginalFilename();
        String suffix = "";
        if (original != null && original.contains(".")) {
            suffix = original.substring(original.lastIndexOf('.'));
        }
        String filename = Instant.now().toEpochMilli() + "_" + Math.abs(original == null ? filenameHash() : original.hashCode()) + suffix;

        Path target = Path.of(uploadsDir, filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String url = base + "/uploads/" + filename;

        return new ApiResponse<>(0, "OK", url);
    }

    private int filenameHash() {
        return (int) (Math.random() * Integer.MAX_VALUE);
    }
}

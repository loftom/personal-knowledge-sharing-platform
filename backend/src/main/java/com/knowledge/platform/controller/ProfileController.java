package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.security.UserContext;
import com.knowledge.platform.service.AnalyticsService;
import com.knowledge.platform.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AnalyticsService analyticsService;

    public ProfileController(ProfileService profileService, AnalyticsService analyticsService) {
        this.profileService = profileService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/me/space")
    public ApiResponse<Phase2Dtos.PersonalSpaceResponse> mySpace() {
        return ApiResponse.ok(profileService.space(UserContext.getUserId()));
    }

    @GetMapping("/me/report")
    public ApiResponse<Phase2Dtos.AnalyticsReportResponse> myReport() {
        return ApiResponse.ok(analyticsService.personalReport(UserContext.getUserId()));
    }

    @GetMapping("/me/influence-report")
    public ApiResponse<Phase2Dtos.InfluenceReportResponse> myInfluenceReport() {
        return ApiResponse.ok(analyticsService.influenceReport(UserContext.getUserId()));
    }

    @PutMapping("/me/nickname")
    public ApiResponse<Phase2Dtos.UserSimple> updateNickname(@Valid @RequestBody Phase2Dtos.UpdateNicknameRequest request) {
        return ApiResponse.ok(profileService.updateNickname(UserContext.getUserId(), request.getNickname()));
    }

    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @PostMapping("/me/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("头像文件不能超过2MB");
        }
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("头像仅支持 JPG、PNG、GIF、WebP 格式");
        }
        try {
            String filename = "avatar_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path uploadDir = Paths.get("uploads", "avatars");
            Files.createDirectories(uploadDir);
            file.transferTo(uploadDir.resolve(filename).toFile());
            String url = "/uploads/avatars/" + filename;
            profileService.updateAvatar(UserContext.getUserId(), url);
            return ApiResponse.ok(Map.of("url", url));
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/me/bio")
    public ApiResponse<Void> updateBio(@RequestBody Map<String, String> body) {
        profileService.updateBio(UserContext.getUserId(), body.get("bio"));
        return ApiResponse.ok();
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody Phase2Dtos.ResetPasswordRequest request) {
        profileService.resetPassword(UserContext.getUserId(), request.getNewPassword());
        return ApiResponse.ok();
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount() {
        profileService.deactivateAccount(UserContext.getUserId());
        return ApiResponse.ok();
    }

    @GetMapping("/{userId}/space")
    public ApiResponse<Phase2Dtos.PersonalSpaceResponse> space(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.space(userId));
    }

    @GetMapping("/{userId}/followers")
    public ApiResponse<List<Phase2Dtos.UserSimple>> followers(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.followers(userId));
    }

    @GetMapping("/{userId}/followings")
    public ApiResponse<List<Phase2Dtos.UserSimple>> followings(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.followings(userId));
    }
}

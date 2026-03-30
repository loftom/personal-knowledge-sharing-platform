package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.security.UserContext;
import com.zhihu.platform.service.AnalyticsService;
import com.zhihu.platform.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PutMapping("/me/nickname")
    public ApiResponse<Phase2Dtos.UserSimple> updateNickname(@Valid @RequestBody Phase2Dtos.UpdateNicknameRequest request) {
        return ApiResponse.ok(profileService.updateNickname(UserContext.getUserId(), request.getNickname()));
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

package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.security.UserContext;
import com.zhihu.platform.service.FollowService;
import com.zhihu.platform.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;
    private final ProfileService profileService;

    public FollowController(FollowService followService, ProfileService profileService) {
        this.followService = followService;
        this.profileService = profileService;
    }

    @PostMapping("/{targetUserId}/toggle")
    public ApiResponse<Map<String, Boolean>> toggle(@PathVariable Long targetUserId) {
        boolean following = followService.toggleFollow(targetUserId);
        return ApiResponse.ok(Map.of("following", following));
    }

    @GetMapping("/followers")
    public ApiResponse<List<Phase2Dtos.UserSimple>> myFollowers() {
        return ApiResponse.ok(profileService.followers(UserContext.getUserId()));
    }

    @GetMapping("/following")
    public ApiResponse<List<Phase2Dtos.UserSimple>> myFollowing() {
        return ApiResponse.ok(profileService.followings(UserContext.getUserId()));
    }
}

package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/manage")
public class AdminManageController {

    private final AdminService adminService;

    public AdminManageController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Phase2Dtos.AdminUserItem>> users(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(adminService.users(keyword, status));
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId,
                                              @Valid @RequestBody Phase2Dtos.UpdateUserStatusRequest request) {
        adminService.updateUserStatus(userId, request.getStatus());
        return ApiResponse.ok();
    }

    @GetMapping("/contents")
    public ApiResponse<List<Phase2Dtos.AdminContentItem>> contents(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) String type,
                                                                   @RequestParam(required = false) Long authorId) {
        return ApiResponse.ok(adminService.manageContents(keyword, status, type, authorId));
    }

    @PostMapping("/contents/{contentId}/status")
    public ApiResponse<Void> updateContentStatus(@PathVariable Long contentId,
                                                 @Valid @RequestBody Phase2Dtos.UpdateContentStatusRequest request) {
        adminService.updateContentStatus(contentId, request.getStatus(), request.getReason());
        return ApiResponse.ok();
    }
}
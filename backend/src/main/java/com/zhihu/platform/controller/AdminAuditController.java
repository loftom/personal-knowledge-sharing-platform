package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.Content;
import com.zhihu.platform.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AdminService adminService;

    public AdminAuditController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<Content>> pendingList() {
        return ApiResponse.ok(adminService.pendingList());
    }

    @GetMapping("/list")
    public ApiResponse<List<Content>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listByStatus(status));
    }

    @GetMapping("/overview")
    public ApiResponse<Phase2Dtos.AuditOverview> overview() {
        return ApiResponse.ok(adminService.overview());
    }

    @PostMapping("/{contentId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long contentId) {
        adminService.approve(contentId);
        return ApiResponse.ok();
    }

    @PostMapping("/{contentId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long contentId,
                                    @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        adminService.reject(contentId, reason);
        return ApiResponse.ok();
    }

    @PostMapping("/{contentId}/offline")
    public ApiResponse<Void> offline(@PathVariable Long contentId,
                                     @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        adminService.offline(contentId, reason);
        return ApiResponse.ok();
    }
}

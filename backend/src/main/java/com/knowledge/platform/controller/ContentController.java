package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ContentDtos.CreateContentRequest request) {
        return ApiResponse.ok(contentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ContentDtos.UpdateContentRequest request) {
        contentService.update(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contentService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<Content> detail(@PathVariable Long id,
                                       @RequestParam(defaultValue = "true") boolean incrementView) {
        return ApiResponse.ok(contentService.detail(id, incrementView));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ContentDtos.ContentListItem>> mine(@RequestParam(required = false) String status) {
        return ApiResponse.ok(contentService.myContents(status));
    }

    @GetMapping("/{id}/audit-logs")
    public ApiResponse<List<Phase2Dtos.AuditLogItem>> auditLogs(@PathVariable Long id) {
        return ApiResponse.ok(contentService.auditLogs(id));
    }
}

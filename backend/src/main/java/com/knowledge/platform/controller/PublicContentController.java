package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/content")
public class PublicContentController {

    private final ContentService contentService;

    public PublicContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{id}")
    public ApiResponse<Content> detail(@PathVariable Long id,
                                       @RequestParam(defaultValue = "true") boolean incrementView) {
        return ApiResponse.ok(contentService.detail(id, incrementView));
    }
}

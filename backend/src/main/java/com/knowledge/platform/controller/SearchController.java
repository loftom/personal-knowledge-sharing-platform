package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/public/search")
public class SearchController {

    private final ContentService contentService;

    public SearchController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ApiResponse<ContentDtos.SearchResult> search(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) Long tagId,
                                                         @RequestParam(required = false) String sortBy,
                                                         @RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate, fmt) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate, fmt) : null;
        return ApiResponse.ok(contentService.search(keyword, categoryId, tagId, sortBy, start, end, page, size));
    }
}

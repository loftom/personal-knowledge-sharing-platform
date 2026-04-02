package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/search")
public class SearchController {

    private final ContentService contentService;

    public SearchController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public ApiResponse<List<ContentDtos.ContentListItem>> search(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Long categoryId,
                                                                 @RequestParam(required = false) Long tagId,
                                                                 @RequestParam(required = false) String sortBy,
                                                                 @RequestParam(required = false) Integer page,
                                                                 @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(contentService.search(keyword, categoryId, tagId, sortBy, page, size));
    }
}

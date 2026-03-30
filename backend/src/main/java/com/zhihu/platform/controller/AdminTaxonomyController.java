package com.zhihu.platform.controller;

import com.zhihu.platform.common.ApiResponse;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.service.TaxonomyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/taxonomy")
public class AdminTaxonomyController {

    private final TaxonomyService taxonomyService;

    public AdminTaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @PostMapping("/categories")
    public ApiResponse<Long> createCategory(@Valid @RequestBody Phase2Dtos.CreateCategoryRequest request) {
        return ApiResponse.ok(taxonomyService.createCategory(request));
    }

    @PostMapping("/tags")
    public ApiResponse<Long> createTag(@Valid @RequestBody Phase2Dtos.CreateTagRequest request) {
        return ApiResponse.ok(taxonomyService.createTag(request));
    }
}

package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.service.TaxonomyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/taxonomy")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    public TaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Phase2Dtos.CategoryOption>> categories() {
        return ApiResponse.ok(taxonomyService.categories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<Phase2Dtos.TagOption>> tags() {
        return ApiResponse.ok(taxonomyService.tags());
    }
}

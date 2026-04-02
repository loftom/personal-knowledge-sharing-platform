package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.entity.Comment;
import com.knowledge.platform.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/like/toggle")
    public ApiResponse<Map<String, Boolean>> toggleLike(@Valid @RequestBody ContentDtos.ToggleRequest request) {
        boolean liked = interactionService.toggleLike(request);
        return ApiResponse.ok(Map.of("liked", liked));
    }

    @PostMapping("/favorite/{contentId}/toggle")
    public ApiResponse<Map<String, Boolean>> toggleFavorite(@PathVariable Long contentId) {
        boolean favorited = interactionService.toggleFavorite(contentId);
        return ApiResponse.ok(Map.of("favorited", favorited));
    }

    @PostMapping("/comment/{contentId}")
    public ApiResponse<Long> createComment(@PathVariable Long contentId,
                                           @Valid @RequestBody ContentDtos.CommentRequest request) {
        return ApiResponse.ok(interactionService.comment(contentId, request));
    }

    @GetMapping("/comment/{contentId}")
    public ApiResponse<List<ContentDtos.CommentView>> listComments(@PathVariable Long contentId) {
        return ApiResponse.ok(interactionService.listComments(contentId));
    }
}

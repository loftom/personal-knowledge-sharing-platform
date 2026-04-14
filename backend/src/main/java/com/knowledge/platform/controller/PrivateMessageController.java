package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.service.PrivateMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/message")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    public PrivateMessageController(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Phase2Dtos.PrivateConversationItem>> conversations() {
        return ApiResponse.ok(privateMessageService.conversations());
    }

    @GetMapping("/with/{userId}")
    public ApiResponse<List<Phase2Dtos.PrivateMessageItem>> messages(@PathVariable Long userId) {
        return ApiResponse.ok(privateMessageService.messagesWith(userId));
    }

    @PostMapping
    public ApiResponse<Phase2Dtos.PrivateMessageItem> send(@Valid @RequestBody Phase2Dtos.SendPrivateMessageRequest request) {
        return ApiResponse.ok(privateMessageService.send(request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Phase2Dtos.PrivateMessageUnreadResponse> unreadCount() {
        return ApiResponse.ok(privateMessageService.unreadCount());
    }
}

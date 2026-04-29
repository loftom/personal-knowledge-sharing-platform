package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.entity.SensitiveWord;
import com.knowledge.platform.security.UserContext;
import com.knowledge.platform.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sensitive-word")
public class SensitiveWordController {

    private final AuditService auditService;

    public SensitiveWordController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<SensitiveWord>> list() {
        requireAdmin();
        return ApiResponse.ok(auditService.listWords());
    }

    @PostMapping
    public ApiResponse<SensitiveWord> add(@RequestBody Map<String, String> body) {
        requireAdmin();
        return ApiResponse.ok(auditService.addWord(body.get("word")));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        requireAdmin();
        String word = body.containsKey("word") ? (String) body.get("word") : null;
        Integer enabled = body.containsKey("enabled") ? ((Number) body.get("enabled")).intValue() : null;
        auditService.updateWord(id, word, enabled);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requireAdmin();
        auditService.deleteWord(id);
        return ApiResponse.ok();
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new AppException("Admin role required");
        }
    }
}

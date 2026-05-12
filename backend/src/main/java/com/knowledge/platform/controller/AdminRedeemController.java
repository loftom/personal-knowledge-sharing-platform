package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.entity.RedeemItem;
import com.knowledge.platform.service.RedeemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/redeem")
public class AdminRedeemController {

    private final RedeemService redeemService;

    public AdminRedeemController(RedeemService redeemService) {
        this.redeemService = redeemService;
    }

    @GetMapping("/items")
    public ApiResponse<List<RedeemItem>> allItems() {
        return ApiResponse.ok(redeemService.allItems());
    }

    @PostMapping("/items")
    public ApiResponse<RedeemItem> addItem(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Integer pointCost = body.get("pointCost") != null ? ((Number) body.get("pointCost")).intValue() : null;
        String icon = (String) body.get("icon");
        return ApiResponse.ok(redeemService.addItem(name, description, pointCost, icon));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Integer pointCost = body.get("pointCost") != null ? ((Number) body.get("pointCost")).intValue() : null;
        String icon = (String) body.get("icon");
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : null;
        redeemService.updateItem(id, name, description, pointCost, icon, enabled);
        return ApiResponse.ok();
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        redeemService.deleteItem(id);
        return ApiResponse.ok();
    }
}

package com.knowledge.platform.controller;

import com.knowledge.platform.common.ApiResponse;
import com.knowledge.platform.domain.entity.RedeemItem;
import com.knowledge.platform.domain.entity.RedeemRecord;
import com.knowledge.platform.service.RedeemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redeem")
public class RedeemController {

    private final RedeemService redeemService;

    public RedeemController(RedeemService redeemService) {
        this.redeemService = redeemService;
    }

    @GetMapping("/items")
    public ApiResponse<List<RedeemItem>> availableItems() {
        return ApiResponse.ok(redeemService.availableItems());
    }

    @PostMapping("/{itemId}")
    public ApiResponse<Void> redeem(@PathVariable Long itemId) {
        redeemService.redeem(itemId);
        return ApiResponse.ok();
    }

    @GetMapping("/records")
    public ApiResponse<List<RedeemRecord>> myRecords() {
        return ApiResponse.ok(redeemService.myRedeemRecords());
    }

}

package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.dto.AdminDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ItemService;
import com.campus.trade.vo.AdminItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
public class AdminItemController {

    private final ItemService itemService;

    @GetMapping
    public ApiResponse<PageResult<AdminItemVO>> list(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(itemService.adminSearch(status, keyword, page, size));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AdminItemVO> approve(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(itemService.approve(id, SecurityUtils.currentId()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AdminItemVO> reject(@PathVariable Long id,
                                           @Valid @RequestBody AdminDtos.RejectItemRequest request) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(itemService.reject(id, request.reason(), SecurityUtils.currentId()));
    }

    @PostMapping("/{id}/takedown")
    public ApiResponse<AdminItemVO> takedown(@PathVariable Long id,
                                             @Valid @RequestBody AdminDtos.TakedownRequest request) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(itemService.takedown(id, request.reason(), request.ruleBasis(),
                SecurityUtils.currentId()));
    }
}

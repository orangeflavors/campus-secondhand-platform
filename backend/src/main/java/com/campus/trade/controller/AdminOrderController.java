package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.dto.OrderDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.OrderService;
import com.campus.trade.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<OrderVO>> disputes(@RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        SecurityUtils.requireAdmin();
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        List<OrderVO> records = orderService.listDisputes(p, s);
        return ApiResponse.ok(PageResult.of(records, orderService.countDisputes(), p, s));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<OrderVO> resolve(@PathVariable Long id,
                                        @Valid @RequestBody OrderDtos.ResolveDisputeRequest request) {
        Long adminId = SecurityUtils.requireAdmin().id();
        return ApiResponse.ok(orderService.adminResolveDispute(id, request.outcome(), request.reason(), adminId));
    }
}

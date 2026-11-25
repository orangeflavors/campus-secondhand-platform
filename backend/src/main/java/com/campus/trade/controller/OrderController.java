package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.OrderDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.security.UserPrincipal;
import com.campus.trade.service.OrderService;
import com.campus.trade.vo.HandoverLogVO;
import com.campus.trade.vo.OrderVO;
import com.campus.trade.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderVO> create(@Valid @RequestBody OrderDtos.CreateOrderRequest request) {
        UserPrincipal principal = SecurityUtils.requireTradeable();
        return ApiResponse.ok(orderService.create(principal.id(), request.itemId(), request.remark()));
    }

    @GetMapping
    public ApiResponse<List<OrderVO>> list(@RequestParam(required = false) String role,
                                           @RequestParam(required = false) String status) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(orderService.list(SecurityUtils.currentId(), role, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        boolean admin = TradeConst.ROLE_ADMIN.equals(principal.role());
        return ApiResponse.ok(orderService.detail(id, principal.id(), admin));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<OrderVO> accept(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(orderService.accept(id, principal.id()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<OrderVO> reject(@PathVariable Long id,
                                       @Valid @RequestBody OrderDtos.ReasonRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(orderService.reject(id, principal.id(), request.reason()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderVO> cancel(@PathVariable Long id,
                                       @Valid @RequestBody OrderDtos.ReasonRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(orderService.cancel(id, principal.id(), request.reason()));
    }

    @PutMapping("/{id}/handover")
    public ApiResponse<OrderVO> updateHandover(@PathVariable Long id,
                                               @Valid @RequestBody OrderDtos.HandoverRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(orderService.updateHandover(id, principal.id(), request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderVO> confirm(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(orderService.confirm(id, principal.id()));
    }

    @PostMapping("/{id}/dispute")
    public ApiResponse<OrderVO> dispute(@PathVariable Long id,
                                        @Valid @RequestBody OrderDtos.ReasonRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(orderService.dispute(id, principal.id(), request.reason()));
    }

    @GetMapping("/{id}/handover-logs")
    public ApiResponse<List<HandoverLogVO>> handoverLogs(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        boolean admin = TradeConst.ROLE_ADMIN.equals(principal.role());
        return ApiResponse.ok(orderService.handoverLogs(id, principal.id(), admin));
    }

    @PostMapping("/{id}/reviews")
    public ApiResponse<ReviewVO> createReview(@PathVariable Long id,
                                              @Valid @RequestBody OrderDtos.ReviewRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(orderService.createReview(id, principal.id(), request));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<ReviewVO>> reviews(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        boolean admin = TradeConst.ROLE_ADMIN.equals(principal.role());
        return ApiResponse.ok(orderService.reviews(id, principal.id(), admin));
    }
}

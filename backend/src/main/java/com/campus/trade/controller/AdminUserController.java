package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.dto.AdminDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.AdminService;
import com.campus.trade.vo.AdminUserVO;
import com.campus.trade.vo.ModerationActionVO;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String accountStatus,
                                                     @RequestParam(required = false) String authStatus,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(adminService.users(keyword, accountStatus, authStatus, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserVO> detail(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(adminService.userDetail(id));
    }

    @GetMapping("/{id}/actions")
    public ApiResponse<List<ModerationActionVO>> actions(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(adminService.userActions(id));
    }

    @PostMapping("/{id}/sanction")
    public ApiResponse<ModerationActionVO> sanction(@PathVariable Long id,
                                                    @Valid @RequestBody AdminDtos.SanctionRequest request) {
        Long adminId = SecurityUtils.requireAdmin().id();
        return ApiResponse.ok(adminService.sanction(id, adminId, request));
    }
}

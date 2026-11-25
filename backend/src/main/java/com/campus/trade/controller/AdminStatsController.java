package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.AdminService;
import com.campus.trade.service.StatsService;
import com.campus.trade.vo.AuditLogVO;
import com.campus.trade.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;
    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse<StatsVO> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(statsService.build(from, to));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLogVO>> auditLogs(@RequestParam(required = false) String action,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(adminService.auditLogs(action, page, size));
    }
}

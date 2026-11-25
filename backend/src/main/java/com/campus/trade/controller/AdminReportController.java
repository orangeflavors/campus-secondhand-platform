package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.dto.AdminDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.ReportService;
import com.campus.trade.vo.ReportVO;
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
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<PageResult<ReportVO>> list(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(reportService.adminList(status, page, size));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<ReportVO> claim(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(reportService.claim(id, SecurityUtils.currentId()));
    }

    @PostMapping("/{id}/handle")
    public ApiResponse<ReportVO> handle(@PathVariable Long id,
                                        @Valid @RequestBody AdminDtos.HandleReportRequest request) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(reportService.handle(id, SecurityUtils.currentId(), request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ReportVO> close(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(reportService.close(id, SecurityUtils.currentId()));
    }

    @PostMapping("/{id}/appeal/resolve")
    public ApiResponse<ReportVO> resolveAppeal(@PathVariable Long id,
                                               @Valid @RequestBody AdminDtos.ResolveAppealRequest request) {
        SecurityUtils.requireAdmin();
        return ApiResponse.ok(reportService.resolveAppeal(id, SecurityUtils.currentId(), request));
    }
}

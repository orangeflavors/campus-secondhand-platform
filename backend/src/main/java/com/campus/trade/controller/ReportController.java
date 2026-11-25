package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.ReportDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.security.UserPrincipal;
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

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ApiResponse<ReportVO> create(@Valid @RequestBody ReportDtos.CreateReportRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(reportService.create(principal.id(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<ReportVO>> mine(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(reportService.mine(SecurityUtils.currentId(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportVO> detail(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        boolean admin = TradeConst.ROLE_ADMIN.equals(principal.role());
        return ApiResponse.ok(reportService.detail(id, principal.id(), admin));
    }

    @PostMapping("/{id}/appeal")
    public ApiResponse<ReportVO> appeal(@PathVariable Long id,
                                        @Valid @RequestBody ReportDtos.AppealRequest request) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(reportService.appeal(id, principal.id(), request.reason()));
    }
}

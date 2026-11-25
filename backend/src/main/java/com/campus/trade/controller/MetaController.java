package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.service.MetaService;
import com.campus.trade.vo.CategoryVO;
import com.campus.trade.vo.MetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    @GetMapping
    public ApiResponse<MetaVO> meta() {
        return ApiResponse.ok(metaService.meta());
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(metaService.categories());
    }
}

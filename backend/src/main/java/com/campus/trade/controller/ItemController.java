package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.ItemDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.security.UserPrincipal;
import com.campus.trade.service.ItemService;
import com.campus.trade.vo.FavoriteStateVO;
import com.campus.trade.vo.ItemCardVO;
import com.campus.trade.vo.ItemDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public ApiResponse<PageResult<ItemCardVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(itemService.search(keyword, categoryId, minPrice, maxPrice,
                condition, area, sort, page, size));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<ItemDetailVO> detail(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        boolean admin = TradeConst.ROLE_ADMIN.equals(principal.role());
        return ApiResponse.ok(itemService.detail(id, principal.id(), admin));
    }

    @PostMapping("/items")
    public ApiResponse<ItemDetailVO> create(@Valid @RequestBody ItemDtos.ItemSaveRequest request) {
        UserPrincipal principal = SecurityUtils.requireTradeable();
        return ApiResponse.ok(itemService.create(principal.id(), request));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<ItemDetailVO> update(@PathVariable Long id,
                                            @Valid @RequestBody ItemDtos.ItemSaveRequest request) {
        UserPrincipal principal = SecurityUtils.requireTradeable();
        return ApiResponse.ok(itemService.update(id, principal.id(), request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        itemService.delete(id, principal.id());
        return ApiResponse.ok();
    }

    @PostMapping("/items/{id}/submit")
    public ApiResponse<ItemDetailVO> submit(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireTradeable();
        return ApiResponse.ok(itemService.submit(id, principal.id()));
    }

    @PostMapping("/items/{id}/off-shelf")
    public ApiResponse<ItemDetailVO> offShelf(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        return ApiResponse.ok(itemService.offShelf(id, principal.id()));
    }

    @GetMapping("/items/{id}/favorite")
    public ApiResponse<FavoriteStateVO> favoriteState(@PathVariable Long id) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(itemService.favoriteState(id, SecurityUtils.currentId()));
    }

    @PostMapping("/items/{id}/favorite")
    public ApiResponse<FavoriteStateVO> favorite(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(itemService.favorite(id, principal.id()));
    }

    @DeleteMapping("/items/{id}/favorite")
    public ApiResponse<FavoriteStateVO> unfavorite(@PathVariable Long id) {
        return ApiResponse.ok(itemService.unfavorite(id, SecurityUtils.currentId()));
    }

    @GetMapping("/me/items")
    public ApiResponse<PageResult<ItemCardVO>> myItems(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(itemService.myItems(SecurityUtils.currentId(), status, page, size));
    }

    @GetMapping("/me/favorites")
    public ApiResponse<PageResult<ItemCardVO>> favorites(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(itemService.favorites(SecurityUtils.currentId(), page, size));
    }
}

package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.common.PageResult;
import com.campus.trade.dto.AuthDtos;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.PublicProfileVO;
import com.campus.trade.vo.ReviewVO;
import com.campus.trade.vo.UserVO;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody AuthDtos.ProfileUpdateRequest request) {
        return ApiResponse.ok(userService.updateProfile(SecurityUtils.currentId(), request));
    }

    @PostMapping("/me/campus-auth")
    public ApiResponse<UserVO> campusAuth(@Valid @RequestBody AuthDtos.CampusAuthRequest request) {
        return ApiResponse.ok(userService.campusAuth(SecurityUtils.currentId(), request));
    }

    @GetMapping("/{id}/profile")
    public ApiResponse<PublicProfileVO> profile(@PathVariable Long id) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(userService.publicProfile(id, SecurityUtils.currentId()));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<PageResult<ReviewVO>> reviews(@PathVariable Long id,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(userService.reviews(id, page, size));
    }
}

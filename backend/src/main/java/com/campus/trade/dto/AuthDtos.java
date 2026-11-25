package com.campus.trade.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 认证与账户相关请求体。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入用户名")
            String username,

            @NotBlank(message = "请输入密码")
            String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "请输入用户名")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4-20 位字母、数字或下划线")
            String username,

            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String password,

            @NotBlank(message = "请输入昵称")
            @Size(min = 2, max = 20, message = "昵称长度需为 2-20 个字符")
            String nickname,

            @Size(max = 100, message = "学院名称过长")
            String college,

            @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,

            @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "邮箱格式不正确")
            String email,

            @Pattern(regexp = "^$|^\\d{8,12}$", message = "学号需为 8-12 位数字")
            String studentNo
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "请输入用户名")
            String username,

            @NotBlank(message = "请输入注册时填写的学号")
            String studentNo,

            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String newPassword
    ) {
    }

    public record ProfileUpdateRequest(
            @Size(min = 2, max = 20, message = "昵称长度需为 2-20 个字符")
            String nickname,

            @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
            String phone,

            @Email(message = "邮箱格式不正确")
            String email,

            @Size(max = 100, message = "学院名称过长")
            String college,

            @Size(max = 255, message = "头像地址过长")
            String avatarUrl,

            @Pattern(regexp = "PUBLIC|AUTHENTICATED|PRIVATE", message = "联系方式可见范围不合法")
            String contactVisibility
    ) {
    }

    public record CampusAuthRequest(
            @NotBlank(message = "请输入学号")
            @Pattern(regexp = "^\\d{8,12}$", message = "学号需为 8-12 位数字")
            String studentNo,

            @NotBlank(message = "请输入真实姓名")
            @Size(min = 2, max = 20, message = "真实姓名长度需为 2-20 个字符")
            String realName,

            @NotBlank(message = "请输入学院")
            @Size(max = 100, message = "学院名称过长")
            String college
    ) {
    }
}

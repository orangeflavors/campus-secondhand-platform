package com.campus.trade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResultVO {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserVO user;
}

package com.campus.trade.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务错误码与对应的 HTTP 状态。
 */
@Getter
public enum ErrorCode {

    BAD_REQUEST(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "请先登录"),
    NEED_VERIFIED(40301, HttpStatus.FORBIDDEN, "请先完成校园身份认证"),
    ACCOUNT_RESTRICTED(40302, HttpStatus.FORBIDDEN, "账户状态受限，无法执行该操作"),
    FORBIDDEN(40303, HttpStatus.FORBIDDEN, "无权执行该操作"),
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(40901, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务异常，请稍后重试");

    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}

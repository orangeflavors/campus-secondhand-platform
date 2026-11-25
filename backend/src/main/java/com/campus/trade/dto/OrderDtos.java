package com.campus.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 订单、交付与评价相关请求体。
 */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotNull(message = "请选择商品")
            Long itemId,

            @Size(max = 255, message = "备注不能超过 255 个字符")
            String remark
    ) {
    }

    public record ReasonRequest(
            @NotBlank(message = "请填写原因")
            @Size(min = 2, max = 200, message = "原因需为 2-200 个字符")
            String reason
    ) {
    }

    public record HandoverRequest(
            LocalDateTime handoverTime,

            @Size(max = 50, message = "交付区域过长")
            String area,

            @Size(max = 500, message = "备注不能超过 500 个字符")
            String note
    ) {
    }

    public record ReviewRequest(
            @NotNull(message = "请选择评分")
            @Min(value = 1, message = "评分最低为 1 星")
            @Max(value = 5, message = "评分最高为 5 星")
            Integer rating,

            @Size(max = 500, message = "评价内容不能超过 500 个字符")
            String content
    ) {
    }

    public record ResolveDisputeRequest(
            @NotBlank(message = "请选择裁决结果")
            String outcome,

            @NotBlank(message = "请填写裁决理由")
            @Size(min = 2, max = 500, message = "裁决理由需为 2-500 个字符")
            String reason
    ) {
    }
}

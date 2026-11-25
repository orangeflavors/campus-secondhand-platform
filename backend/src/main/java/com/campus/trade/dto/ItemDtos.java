package com.campus.trade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品相关请求体。
 */
public final class ItemDtos {

    private ItemDtos() {
    }

    public record ItemSaveRequest(
            @NotBlank(message = "请填写商品标题")
            @Size(min = 2, max = 100, message = "标题长度需为 2-100 个字符")
            String title,

            @NotNull(message = "请选择商品分类")
            Long categoryId,

            @NotBlank(message = "请选择成色")
            String condition,

            @NotBlank(message = "请填写商品描述")
            @Size(min = 10, max = 2000, message = "描述需为 10-2000 个字符")
            String description,

            @NotNull(message = "请填写价格")
            @DecimalMin(value = "0.00", message = "价格不能为负数")
            @DecimalMax(value = "999999.99", message = "价格超出允许范围")
            BigDecimal price,

            @NotBlank(message = "请选择交付区域")
            @Size(max = 50, message = "交付区域过长")
            String handoverArea,

            @Size(max = 6, message = "最多上传 6 张图片")
            List<String> images
    ) {
    }
}

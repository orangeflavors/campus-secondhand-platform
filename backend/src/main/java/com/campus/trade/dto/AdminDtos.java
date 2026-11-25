package com.campus.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端审核、举报处理与账户处置请求体。
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record RejectItemRequest(
            @NotBlank(message = "请填写驳回原因")
            @Size(min = 2, max = 200, message = "驳回原因需为 2-200 个字符")
            String reason
    ) {
    }

    public record TakedownRequest(
            @NotBlank(message = "请填写下架理由")
            @Size(min = 2, max = 500, message = "下架理由需为 2-500 个字符")
            String reason,

            @Size(max = 500, message = "规则依据过长")
            String ruleBasis
    ) {
    }

    public record SanctionRequest(
            @NotBlank(message = "请选择处置动作")
            String actionType,

            @NotBlank(message = "请填写处置理由")
            @Size(min = 2, max = 500, message = "处置理由需为 2-500 个字符")
            String reason,

            @Size(max = 500, message = "规则依据过长")
            String ruleBasis,

            @Min(value = 1, message = "时长至少 1 天")
            @Max(value = 3650, message = "时长不能超过 3650 天")
            Integer durationDays
    ) {
    }

    public record HandleReportRequest(
            @NotBlank(message = "请选择处理结论")
            String result,

            @Size(max = 500, message = "规则依据过长")
            String ruleBasis,

            @NotBlank(message = "请填写处理理由")
            @Size(min = 2, max = 500, message = "处理理由需为 2-500 个字符")
            String reason,

            String actionType,

            @Min(value = 1, message = "时长至少 1 天")
            @Max(value = 3650, message = "时长不能超过 3650 天")
            Integer durationDays
    ) {
    }

    public record ResolveAppealRequest(
            Boolean uphold,

            @NotBlank(message = "请填写复核理由")
            @Size(min = 2, max = 500, message = "复核理由需为 2-500 个字符")
            String reason
    ) {
    }
}

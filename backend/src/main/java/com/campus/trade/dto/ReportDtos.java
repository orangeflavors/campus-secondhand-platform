package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 举报相关请求体。
 */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record CreateReportRequest(
            @NotBlank(message = "请选择举报对象类型")
            String targetType,

            @NotNull(message = "请选择举报对象")
            Long targetId,

            @NotBlank(message = "请选择举报类别")
            String category,

            @NotBlank(message = "请填写举报说明")
            @Size(min = 5, max = 1000, message = "举报说明需为 5-1000 个字符")
            String description
    ) {
    }

    public record AppealRequest(
            @NotBlank(message = "请填写申诉理由")
            @Size(min = 2, max = 500, message = "申诉理由需为 2-500 个字符")
            String reason
    ) {
    }
}

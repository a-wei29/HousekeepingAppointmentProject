package com.gk.study.requestEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单状态更新请求")
public class UpdateOrderStatusRequest {
    @Schema(description = "订单ID", required = true, example = "123")
    private Long orderId;

    @Schema(description = "状态码（对应 OrderStatus 枚举中的 code）", required = true, example = "0")
    private String status;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
package com.gk.study.enums;


public enum OrderStatus {
    WAITING(0, "待接单"),
    ACCEPTED(1, "已接单"),
    IN_PROGRESS(2, "执行中"),
    SERVICE_COMPLETED(3, "服务完成"),
    USER_REVIEW(4, "用户审核"),
    USER_PAYMENT(5, "用户付款"),
    ORDER_FINISHED(6, "订单结束");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 用于校验传入的状态码是否在枚举中存在
    public static boolean isValid(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    // 根据 code 获取枚举实例
    public static OrderStatus getByCode(int code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}

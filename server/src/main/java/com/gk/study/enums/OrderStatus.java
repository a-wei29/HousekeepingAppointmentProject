package com.gk.study.enums;


public enum OrderStatus {
    WAITING(0, "待接单"),
    WAIT_SERVICE(1, "待服务"),
    IN_PROGRESS(2, "执行中"),
    WAIT_AUDIT(3, "待审核"),
    WAIT_PAYMENT(4, "待付款"),
    WAIT_FOR_COMMENT(5, "待评论"),
    ORDER_FINISHED(6, "已完成");

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

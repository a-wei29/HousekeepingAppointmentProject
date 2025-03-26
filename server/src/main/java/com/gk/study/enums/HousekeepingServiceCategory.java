package com.gk.study.enums;

public enum HousekeepingServiceCategory {
    CLEANING(1, "家政保洁"),
    NURSING(2, "月嫂服务"),
    BABYCARE(3, "育婴服务"),
    DOMESTIC_HELP(4, "保姆服务"),
    HOURLY_SERVICE(5, "钟点工服务"),
    APPLIANCE_REPAIR(6, "家电维修"),
    AIR_CONDITIONER_CLEANING(7, "空调清洗"),
    PIPELINE_UNBLOCKING(8, "管道疏通");

    private final int code;
    private final String description;

    HousekeepingServiceCategory(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 用于校验传入的分类ID是否在枚举中存在
    public static boolean isValid(int code) {
        for (HousekeepingServiceCategory category : HousekeepingServiceCategory.values()) {
            if (category.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    // 根据 code 获取枚举实例
    public static HousekeepingServiceCategory getByCode(int code) {
        for (HousekeepingServiceCategory category : HousekeepingServiceCategory.values()) {
            if (category.getCode() == code) {
                return category;
            }
        }
        return null;
    }
}
package com.gk.study.requestEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户列表查询请求体")
public class UserListRequest {
    @Schema(description = "用户角色", example = "1")
    private Integer role;

    @Schema(description = "手机号模糊匹配", example = "1234567890")
    private String mobile;


    // Getter 和 Setter 方法
    public Integer getRole() {
        return role;
    }
    public void setRole(Integer role) {
        this.role = role;
    }
    public String getMobile() {
        return mobile;
    }
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}

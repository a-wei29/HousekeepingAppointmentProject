package com.gk.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("b_user_thing_history")
public class UserThingHistory implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    public Long id;

    @TableField("user_id")
    public String userId;

    @TableField("thing_id")
    public String thingId;

    @TableField("browse_duration")
    public String browseDuration;

    @TableField("browse_time")
    public String browseTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public String getBrowseDuration() {
        return browseDuration;
    }

    public void setBrowseDuration(String browseDuration) {
        this.browseDuration = browseDuration;
    }

    public String getBrowseTime() {
        return browseTime;
    }

    public void setBrowseTime(String browseTime) {
        this.browseTime = browseTime;
    }
}

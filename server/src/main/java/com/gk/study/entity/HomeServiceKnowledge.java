package com.gk.study.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("b_home_service_knowledge")
public class HomeServiceKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String question;
    private String answer;
    private String createTime;
    private String updateTime;
}

package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.UserThingHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserThingHistoryMapper extends BaseMapper<UserThingHistory> {
    // 如有需要，可在此添加其他自定义查询方法
}
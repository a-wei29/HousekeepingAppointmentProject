package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.CategoryWeight;
import com.gk.study.entity.UserThingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserThingHistoryMapper extends BaseMapper<UserThingHistory> {


    /**
     * 获取用户最近浏览/下单的服务ID列表
     * @param userId 用户ID
     * @param limit  最多返回数量
     */
    List<Long> findRecentThingIds(@Param("userId") Long userId,
                                  @Param("limit")  int limit);

    /**
     * 按分类统计用户对各类别的累计权重
     * @param userId 用户ID
     * @return 列表，包含分类ID与对应的权重
     */
    List<CategoryWeight> fetchCategoryWeights(@Param("userId") Long userId);
    // 如有需要，可在此添加其他自定义查询方法
}
package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.Thing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ThingMapper extends BaseMapper<Thing> {
    /**
     * 查询所有状态为激活（status='1'）的服务
     */
    List<Thing> findAllActive();

    /**
     * 按热度排名查询前 N 的服务
     * @param limit 返回数量
     */
    List<Thing> findTopHot(@Param("limit") int limit);

    /**
     * 根据 ID 列表批量查询服务
     * @param ids 服务ID列表
     */
    List<Thing> findByIds(@Param("ids") List<Long> ids);

}

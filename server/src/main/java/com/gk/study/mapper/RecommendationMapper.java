package com.gk.study.mapper;

import com.gk.study.entity.ItemSim;
import com.gk.study.entity.UserThingScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface RecommendationMapper {
    void deleteAllItemSimilarity();
    List<UserThingScore> fetchUserThingWeights();
    void batchInsertItemSim(@Param("list") List<ItemSim> sims);

    /**
     * 查询与指定服务最相似的邻居服务ID列表
     * @param thingId 源服务ID
     * @param limit   最大返回数量
     */
    List<Long> findTopNSimilar(@Param("thingId") Long thingId,
                               @Param("limit")   int limit);
}


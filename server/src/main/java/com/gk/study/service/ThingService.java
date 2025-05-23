package com.gk.study.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.Thing;

import java.math.BigDecimal;
import java.util.List;

public interface ThingService {
    List<Thing> getThingList(String keyword, String sort, String c, String tag);

//    List<Thing> getThingListNew(String keyword, String sort, Long classificationId, Long tag,
//                                Double userLat, Double userLng, Double distanceKm,
//                                BigDecimal minPrice, BigDecimal maxPrice, Integer minScore);

    IPage<Thing> getThingListNew(String keyword, String sort, Long classificationId, Long tag,
                                 Double userLat, Double userLng, Double distanceKm,
                                 BigDecimal minPrice, BigDecimal maxPrice, Integer minScore,
                                 Page<Thing> pageParam);

    Long createThing(Thing thing);
    void deleteThing(String id);

    void updateThing(Thing thing);

    Thing getThingById(String id);

    void addWishCount(String thingId);

    void addCollectCount(String thingId);

    IPage<Thing> getUserThing(Long userId, Page<Thing> pageParam) ;

    /**
     * 收藏数减1
     * @param thingId 家政服务ID
     */
    void reduceCollectCount(String thingId);
}

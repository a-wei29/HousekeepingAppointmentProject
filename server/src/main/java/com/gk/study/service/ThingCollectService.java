package com.gk.study.service;


import com.gk.study.entity.ThingCollect;

import java.util.List;
import java.util.Map;

public interface ThingCollectService {
    List<Map> getThingCollectList(String userId);
    void createThingCollect(ThingCollect thingCollect);
    int deleteThingCollect(String userId, String thingId);
    ThingCollect getThingCollect(String userId, String thingId);

    /**
     * 统计某个 Thing 被收藏的次数
     */
    int countByThingId(String thingId);
}

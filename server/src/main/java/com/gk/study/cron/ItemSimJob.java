package com.gk.study.cron;

import com.gk.study.entity.ItemSim;
import com.gk.study.entity.UserThingScore;
import com.gk.study.mapper.RecommendationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ItemSimJob {

    @Autowired
    private RecommendationMapper recMapper;

    // 每天凌晨 2 点计算
    @Scheduled(cron = "0 0 2 * * ?")
    public void computeItemSimilarity() {
        // 1. 清空旧表
        recMapper.deleteAllItemSimilarity();
        // 2. 按用户聚合浏览与下单，输出 (user_id, thing_id, weight)
        List<UserThingScore> uList = recMapper.fetchUserThingWeights();
        // 3. 构造 Map<thingId, Map<userId, weight>>
        Map<Long, Map<Long, Double>> itemUserMap = new HashMap<>();
        for (UserThingScore uts : uList) {
            itemUserMap
                    .computeIfAbsent(uts.getThingId(), k->new HashMap<>())
                    .put(uts.getUserId(), uts.getWeight());
        }
        // 4. 计算两两物品余弦
        List<ItemSim> sims = new ArrayList<>();
        List<Long> thingIds = new ArrayList<>(itemUserMap.keySet());
        for (int i = 0; i < thingIds.size(); i++) {
            Long t1 = thingIds.get(i);
            Map<Long, Double> uMap1 = itemUserMap.get(t1);
            double norm1 = uMap1.values().stream().mapToDouble(w->w*w).sum();
            for (int j = i+1; j < thingIds.size(); j++) {
                Long t2 = thingIds.get(j);
                Map<Long, Double> uMap2 = itemUserMap.get(t2);
                // 计算 dot
                double dot = 0;
                for (Long u : uMap1.keySet()) {
                    dot += uMap1.get(u) * Optional.ofNullable(uMap2.get(u)).orElse(0.0);
                }
                double norm2 = uMap2.values().stream().mapToDouble(w->w*w).sum();
                if (dot > 0) {
                    double sim = dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
                    sims.add(new ItemSim(t1, t2, sim));
                    sims.add(new ItemSim(t2, t1, sim));
                }
            }
        }
        // 5. 批量插入相似度
        recMapper.batchInsertItemSim(sims);
    }
}


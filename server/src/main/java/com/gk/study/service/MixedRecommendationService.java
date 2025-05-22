package com.gk.study.service;

import com.gk.study.entity.CategoryWeight;
import com.gk.study.entity.Thing;
import com.gk.study.mapper.RecommendationMapper;
import com.gk.study.mapper.ThingMapper;
import com.gk.study.mapper.UserThingHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MixedRecommendationService {

    @Autowired
    private RecommendationMapper recMapper;
    @Autowired private ThingMapper thingMapper;
    @Autowired private UserThingHistoryMapper historyMapper;

    // 最终返回 topN
    private static final int FINAL_TOPN = 20;
    private static final int CF_NEIGHBORS = 5;
    private static final int CONTENT_TOPK = 10;

    public List<Thing> recommendForUser(Long userId) {
        // 1. 取出用户最近 5 次浏览/下单的 Thing
        List<Long> recentThings = historyMapper.findRecentThingIds(userId, 5);

        // 2. 协同过滤召回
        Set<Long> cfSet = new LinkedHashSet<>();
        for (Long tid : recentThings) {
            List<Long> neighbors = recMapper.findTopNSimilar(tid, CF_NEIGHBORS);
            cfSet.addAll(neighbors);
        }

        // 3. 内容召回
        double[] userProfile = buildUserProfile(userId);
        List<Thing> allThings = thingMapper.findAllActive();
        // 按相似度排序
        List<Long> contentRec = allThings.stream()
                .sorted(Comparator.comparingDouble(
                        t -> -cosine(userProfile, t.getFeatureVector())))
                .limit(CONTENT_TOPK)
                .map(Thing::getId)
                .collect(Collectors.toList());

        // 4. 合并 & 去重
        LinkedHashSet<Long> finalSet = new LinkedHashSet<>();
        finalSet.addAll(cfSet);
        finalSet.addAll(contentRec);

         // 5. 热门补充
        if (finalSet.size() < FINAL_TOPN) {
            // findTopHot 返回的是 List<Thing>，我们只需要它们的 ID
            List<Thing> hotThings = thingMapper.findTopHot(FINAL_TOPN - finalSet.size());
            List<Long> hotIds = hotThings.stream()
                    .map(Thing::getId)
                    .collect(Collectors.toList());
            finalSet.addAll(hotIds);
        }

        // 6. 按原集合顺序取前 FINAL_TOPN
        List<Long> resultIds = finalSet.stream().limit(FINAL_TOPN).collect(Collectors.toList());
        return thingMapper.findByIds(resultIds);
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na==0||nb==0 ? 0 : dot/(Math.sqrt(na)*Math.sqrt(nb));
    }

    private double[] buildUserProfile(Long userId) {
        // 简单：按分类聚合浏览／下单权重
         // 简单：按分类聚合浏览／下单权重，先把 List<CategoryWeight> 转成 Map<分类ID, 权重>
        List<CategoryWeight> weights = historyMapper.fetchCategoryWeights(userId);
        Map<Integer, Double> catScore = weights.stream()
                     .collect(Collectors.toMap(CategoryWeight::getClassificationId,
                                               CategoryWeight::getWeight));
        double sum = catScore.values().stream().mapToDouble(Double::doubleValue).sum();
        double[] profile = new double[8];
        for (int i = 1; i <= 8; i++) {
            profile[i-1] = sum>0 ? catScore.getOrDefault(i, 0.0)/sum : 0.0;
        }
        return profile;
    }
}

package com.gk.study.service.sync;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.entity.Thing;
import com.gk.study.mapper.*;
import com.gk.study.retriever.BM25Retriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.gk.study.entity.Order;
import com.gk.study.entity.Comment;
import com.gk.study.entity.Classification;
import com.gk.study.entity.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BusinessDataSync {

    @Autowired
    private ThingMapper thingMapper;

    @Autowired
    private ServiceProviderMapper providerMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ClassificationMapper classificationMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    @Qualifier("businessEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private BM25Retriever bm25Retriever;

    // 用于收集所有同步的数据
    private final List<TextSegment> allBusinessSegments = new ArrayList<>();

    @Scheduled(fixedDelay = 300000)
    public void syncAllBusinessData() {
        log.info("========== 开始同步业务数据到向量库 ==========");

        // 清空上一次的数据
        allBusinessSegments.clear();
        AtomicInteger totalSynced = new AtomicInteger(0);

        try {
            // 按顺序同步，收集所有数据
            totalSynced.addAndGet(syncServiceProviders());
            totalSynced.addAndGet(syncThings());
            totalSynced.addAndGet(syncOrders());
            totalSynced.addAndGet(syncComments());
            totalSynced.addAndGet(syncClassifications());
            totalSynced.addAndGet(syncTags());

            log.info("本次同步完成，共同步 {} 条数据", totalSynced.get());

            // 如果有数据同步，更新 BM25 并刷新索引
            if (!allBusinessSegments.isEmpty()) {
                log.info("更新 BM25 数据，共 {} 条", allBusinessSegments.size());
                bm25Retriever.updateSegments(new ArrayList<>(allBusinessSegments));
                bm25Retriever.refreshIndex();
            }

            log.info("========== 业务数据同步完成 ==========");
        } catch (Exception e) {
            log.error("业务数据同步失败", e);
        }
    }

    // 同步服务项目
    private int syncThings() {
        List<Thing> things = thingMapper.selectList(null);
        log.info("同步服务项目数据: {} 条", things.size());
        int successCount = 0;

        for (Thing thing : things) {
            try {
                String text = String.format(
                        "【类型:服务项目】服务名称：%s，价格：%.2f元/小时，服务描述：%s。服务人员信息：年龄%s岁，性别：%s，联系电话：%s，服务区域：%s。",
                        thing.getTitle(),
                        thing.getPrice(),
                        thing.getDescription(),
                        thing.getAge(),
                        "男".equals(thing.getSex()) ? "男性" : "女性",
                        thing.getMobile(),
                        thing.getLocation()
                );

                TextSegment segment = TextSegment.from(text);

                // 存入向量库
                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                // 收集到 BM25 数据中
                allBusinessSegments.add(segment);

                successCount++;
                log.debug("服务项目同步成功: {}", thing.getTitle());
            } catch (Exception e) {
                log.error("同步服务项目失败, id: {}", thing.getId(), e);
            }
        }

        log.info("服务项目同步完成，成功 {} 条", successCount);
        return successCount;
    }

    // 同步阿姨信息
    private int syncServiceProviders() {
        List<ServiceProvider> providers = providerMapper.selectList(null);
        log.info("同步阿姨信息: {} 条", providers.size());
        int successCount = 0;

        for (ServiceProvider provider : providers) {
            try {
                String status = "1".equals(provider.getStatus()) ? "可接单" : "休息中";
                String text = String.format(
                        "【类型:阿姨信息】姓名：%s，评分：%.2f分，服务状态：%s。个人简介：%s",
                        provider.getName(),
                        provider.getRating(),
                        status,
                        provider.getDescription()
                );

                TextSegment segment = TextSegment.from(text);

                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                allBusinessSegments.add(segment);

                successCount++;
                log.debug("阿姨信息同步成功: {}", provider.getName());
            } catch (Exception e) {
                log.error("同步阿姨信息失败, id: {}", provider.getId(), e);
            }
        }

        log.info("阿姨信息同步完成，成功 {} 条", successCount);
        return successCount;
    }

    // 同步订单
    private int syncOrders() {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.in("status", "2", "3");
        wrapper.orderByDesc("order_time");
        wrapper.last("limit 500");

        List<Order> orders = orderMapper.selectList(wrapper);
        log.info("同步订单数据: {} 条", orders.size());
        int successCount = 0;

        for (Order order : orders) {
            try {
                String statusDesc = getOrderStatusDesc(order.getStatus());
                String text = String.format(
                        "【类型:历史订单】订单号：%s，服务类型ID：%s，订单状态：%s，下单时间：%s，服务区域：%s，备注：%s",
                        order.getOrderNumber(),
                        order.getThingId(),
                        statusDesc,
                        order.getOrderTime(),
                        order.getReceiverAddress(),
                        order.getRemark() != null ? order.getRemark() : ""
                );

                TextSegment segment = TextSegment.from(text);

                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                allBusinessSegments.add(segment);

                successCount++;
                log.debug("订单同步成功: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("同步订单失败, id: {}", order.getId(), e);
            }
        }

        log.info("订单同步完成，成功 {} 条", successCount);
        return successCount;
    }

    // 同步评价
    private int syncComments() {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.ge("rate", 4);
        wrapper.orderByDesc("comment_time");
        wrapper.last("limit 200");

        List<Comment> comments = commentMapper.selectList(wrapper);
        log.info("同步用户评价: {} 条", comments.size());
        int successCount = 0;

        for (Comment comment : comments) {
            try {
                String text = String.format(
                        "【类型:用户评价】对服务ID：%s，评分：%s分，评价内容：%s，评价时间：%s。这是用户对阿姨服务的真实反馈。",
                        comment.getThingId(),
                        comment.getRate(),
                        comment.getContent() != null ? comment.getContent() : "",
                        comment.getCommentTime()
                );

                TextSegment segment = TextSegment.from(text);

                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                allBusinessSegments.add(segment);

                successCount++;
                log.debug("评价同步成功: {}", comment.getId());
            } catch (Exception e) {
                log.error("同步评价失败, id: {}", comment.getId(), e);
            }
        }

        log.info("评价同步完成，成功 {} 条", successCount);
        return successCount;
    }

    // 同步分类
    private int syncClassifications() {
        List<Classification> classifications = classificationMapper.selectList(null);
        log.info("同步服务分类: {} 条", classifications.size());
        int successCount = 0;

        for (Classification c : classifications) {
            try {
                String text = String.format(
                        "【类型:服务分类】分类名称：%s，分类ID：%s。这是家政服务的分类信息。",
                        c.getTitle(),
                        c.getId()
                );
                TextSegment segment = TextSegment.from(text);

                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                allBusinessSegments.add(segment);

                successCount++;
            } catch (Exception e) {
                log.error("同步分类失败, id: {}", c.getId(), e);
            }
        }

        log.info("服务分类同步完成，成功 {} 条", successCount);
        return successCount;
    }

    // 同步标签
    private int syncTags() {
        List<Tag> tags = tagMapper.selectList(null);
        log.info("同步服务标签: {} 条", tags.size());
        int successCount = 0;

        for (Tag tag : tags) {
            try {
                String text = String.format(
                        "【类型:服务标签】标签名称：%s，标签ID：%s。这是阿姨的特殊技能标签，用于阿姨筛选和推荐。",
                        tag.getTitle(),
                        tag.getId()
                );
                TextSegment segment = TextSegment.from(text);

                var embedding = embeddingModel.embed(text).content();
                embeddingStore.add(embedding, segment);

                allBusinessSegments.add(segment);

                successCount++;
            } catch (Exception e) {
                log.error("同步标签失败, id: {}", tag.getId(), e);
            }
        }

        log.info("服务标签同步完成，成功 {} 条", successCount);
        return successCount;
    }

    private String getOrderStatusDesc(String status) {
        switch(status) {
            case "0": return "待支付";
            case "1": return "已支付";
            case "2": return "服务中";
            case "3": return "已完成";
            case "4": return "已取消";
            default: return "未知状态";
        }
    }
}
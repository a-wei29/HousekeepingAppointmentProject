package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.Comment;
import com.gk.study.entity.ServiceProvider;
import com.gk.study.entity.Thing;
import com.gk.study.mapper.CommentMapper;
import com.gk.study.mapper.ServiceProviderMapper;
import com.gk.study.mapper.ThingMapper;
import com.gk.study.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {


    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ThingMapper thingMapper;
    @Autowired
    private ServiceProviderMapper serviceProviderMapper;

    @Override
    public List<Comment> getCommentList() {
        return commentMapper.getList();
    }

    @Override
    public void createComment(Comment comment) {
        System.out.println(comment);
        comment.setCommentTime(String.valueOf(System.currentTimeMillis()));
        commentMapper.insert(comment);
    }

    @Override
    public void deleteComment(String id) {
        commentMapper.deleteById(id);
    }

    @Override
    public void updateComment(Comment comment) {
        commentMapper.updateById(comment);
    }

    @Override
    public Comment getCommentDetail(String id) {
        return commentMapper.selectById(id);
    }

    @Override
    public List<Comment> getThingCommentList(String thingId, String order) {
        return commentMapper.selectThingCommentList(thingId, order);
    }

    @Override
    public List<Comment> getUserCommentList(String userId) {
        return commentMapper.selectUserCommentList(userId);
    }

    @Override
    public Page<Comment> getThingCommentListByRating(String thingId, String order, Integer pageNo, Integer pageSize) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("thing_id", thingId);
        // 按评分（rate）排序：根据order参数选择升序或降序
        if ("asc".equalsIgnoreCase(order)) {
            wrapper.orderByAsc("rate");
        } else {
            wrapper.orderByDesc("rate");
        }
        Page<Comment> page = new Page<>(pageNo, pageSize);
        return commentMapper.selectPage(page, wrapper);
    }


    /**
     * 保存评论，并重算 Thing 与 ServiceProvider 的平均评分
     */
    @Transactional
    @Override
    public void createCommentAndUpdateScores(Comment comment) {
        // —— 1. 插入评论 ——
        commentMapper.insert(comment);

        // —— 2. 重算并更新 Thing 的 score ——
        Double thingAvg = commentMapper.selectAvgRateByThingId(Long.valueOf(comment.getThingId()));
        Thing thing = thingMapper.selectById(comment.getThingId());
        if (thingAvg == null) {
            thingAvg = 0.0;
        }
        // 按需求保留一位小数
        thing.setScore(String.format("%.1f", thingAvg));
        thingMapper.updateById(thing);

        // —— 3. 重算并更新 ServiceProvider 的 rating ——
        //    找到发布该 Thing 的服务提供者
        ServiceProvider sp = serviceProviderMapper.selectOne(
                new QueryWrapper<ServiceProvider>()
                        .eq("user_id", thing.getUserId())
        );
        if (sp != null) {
            Double providerAvg = commentMapper.selectAvgRateByUserId(sp.getUserId());
            if (providerAvg == null) {
                providerAvg = 0.0;
            }
            sp.setRating(BigDecimal.valueOf(providerAvg));
            serviceProviderMapper.updateById(sp);
        }
    }
}

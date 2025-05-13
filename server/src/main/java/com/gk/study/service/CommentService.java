package com.gk.study.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.Comment;

import java.util.List;

public interface CommentService {
    List<Comment> getCommentList();
    void createComment(Comment comment);
    void deleteComment(String id);
    void updateComment(Comment comment);
    Comment getCommentDetail(String id);

    List<Comment> getThingCommentList(String thingId, String order);

    List<Comment> getUserCommentList(String userId);

    /**
     * 分页查询指定商品的评论列表，根据评分排序（升序或降序）
     *
     * @param thingId 商品ID
     * @param order 排序方式，asc 为升序，desc 为降序，默认为降序
     * @param pageNo 页码
     * @param pageSize 每页记录数
     * @return 分页后的评论列表
     */

    public Page<Comment> getThingCommentListByRating(String thingId, String order, Integer pageNo, Integer pageSize);

    public void createCommentAndUpdateScores(Comment comment);
}

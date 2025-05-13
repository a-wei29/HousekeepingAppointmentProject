package com.gk.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.study.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<Comment> getList();

    List<Comment> selectThingCommentList(String thingId, String order);

    List<Comment> selectUserCommentList(String userId);

    // 取某个 Thing（thing_id）的平均评分
    @Select("SELECT AVG(CAST(rate AS DECIMAL(5,2))) FROM b_comment WHERE thing_id = #{thingId}")
    Double selectAvgRateByThingId(@Param("thingId") Long thingId);

    // 取某个服务提供者（userId）名下所有 Thing 的平均评分
    @Select("SELECT AVG(sub.avg_rate) FROM (" +
            "SELECT AVG(CAST(rate AS DECIMAL(5,2))) AS avg_rate " +
            "FROM b_comment " +
            "WHERE thing_id IN (SELECT id FROM b_thing WHERE user_id = #{userId}) " +
            "GROUP BY thing_id" +
            ") sub")
    Double selectAvgRateByUserId(@Param("userId") Long userId);
}

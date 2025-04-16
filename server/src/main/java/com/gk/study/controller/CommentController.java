package com.gk.study.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Comment;
import com.gk.study.entity.User;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.CommentService;
import com.gk.study.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    private final static Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    CommentService service;

    @Autowired
    private UserService userService; // 注入 UserService，用于获取用户详情

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public APIResponse list(){
        List<Comment> list =  service.getCommentList();
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }


    // 所有评论
    @RequestMapping(value = "/listThingComments", method = RequestMethod.GET)
    public APIResponse listThingComments(String thingId, String order){
        List<Comment> list =  service.getThingCommentList(thingId, order);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    // 用户的所有评论
    @RequestMapping(value = "/listUserComments", method = RequestMethod.GET)
    public APIResponse listUserComments(String userId){
        List<Comment> list =  service.getUserCommentList(userId);
        return new APIResponse(ResponeCode.SUCCESS, "查询成功", list);
    }

    @Operation(
            summary = "创建评论",
            description = "前端传入userId、orderId、thingId、评论内容（content）以及评分（rate），创建评论记录，后台设置评论时间。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "创建成功"),
                    @ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<APIResponse<?>> createComment(@RequestBody Comment comment) throws IOException {
        // 设置评论时间为当前系统时间（毫秒级或按照需求格式化为时间字符串）
        comment.setCommentTime(String.valueOf(System.currentTimeMillis()));
        service.createComment(comment);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "创建成功", comment));
    }


    // 删除评论接口（批量删除，根据传入的 ids，多个 id 用逗号分隔）
    @Operation(
            summary = "删除评论",
            description = "根据传入的评论 id 列表进行批量删除操作,只传一个也可以。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功"),
                    @ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<APIResponse<?>> delete(@RequestParam String ids) {
        // 按逗号分隔的 id 进行循环删除
        String[] arr = ids.split(",");
        for (String id : arr) {
            service.deleteComment(id);
        }
        return ResponseEntity.ok(new APIResponse(ResponeCode.SUCCESS, "删除成功"));
    }

    // 更新评论接口
    @Operation(
            summary = "更新评论",
            description = "根据传入的评论对象，更新对应的评论记录。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<APIResponse<?>> update(@RequestBody Comment comment) throws IOException {
        service.updateComment(comment);
        return ResponseEntity.ok(new APIResponse(ResponeCode.SUCCESS, "更新成功"));
    }

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @Transactional
    public APIResponse like(String id) throws IOException {
        Comment commentBean = service.getCommentDetail(id);
        int likeCount = Integer.parseInt(commentBean.getLikeCount()) + 1;
        commentBean.setLikeCount(String.valueOf(likeCount));
        service.updateComment(commentBean);
        return new APIResponse(ResponeCode.SUCCESS, "更新成功");
    }
    @Operation(
            summary = "分页查询商品评论（按评分排序）",
            description = "通过thingId获取对应评论，支持传入order参数控制评分（rate）的升序(asc)或降序排序(desc)，并分页返回评论数据。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @GetMapping("/listThingCommentsByRating")
    public ResponseEntity<APIResponse<?>> listThingCommentsByRating(
            @RequestParam String thingId,
            @RequestParam(defaultValue = "desc",required = false) String order,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Comment> page = service.getThingCommentListByRating(thingId, order, pageNo, pageSize);

        // 遍历查询结果，根据comment中的userId调用UserService获取用户详情，并填充username字段
        if (page.getRecords() != null) {
            for (Comment comment : page.getRecords()) {
                User user = userService.getUserDetail(comment.getUserId());
                if (user != null) {
                    comment.setUsername(user.getUsername());
                }
            }
        }
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", page));
    }
}

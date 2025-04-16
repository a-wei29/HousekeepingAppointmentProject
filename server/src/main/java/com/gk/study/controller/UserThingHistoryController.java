package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.UserThingHistory;
import com.gk.study.service.UserThingHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/userThingHistory")
public class UserThingHistoryController {

    private final static Logger logger = LoggerFactory.getLogger(UserThingHistoryController.class);

    @Autowired
    private UserThingHistoryService userThingHistoryService;

    @Operation(
            summary = "记录用户浏览家政服务历史",
            description = "前端传递用户浏览家政服务的历史记录，包括 userId、thingId、浏览时长及浏览时间点，将记录写入数据库；若浏览时间点未传入，则后台自动设置为当前系统时间。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "记录创建成功"),
                    @ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<APIResponse<?>> createUserThingHistory(@RequestBody UserThingHistory history) {
        // 如果前端未传入 browseTime，则后台设置为当前时间
        if(history.getBrowseTime() == null || history.getBrowseTime().isEmpty()){
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            history.setBrowseTime(currentTime);
        }
        userThingHistoryService.createHistory(history);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "记录创建成功", history));
    }
}
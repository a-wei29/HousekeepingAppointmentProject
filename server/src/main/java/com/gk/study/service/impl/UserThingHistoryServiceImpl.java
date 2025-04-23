package com.gk.study.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.UserThingHistory;
import com.gk.study.mapper.UserThingHistoryMapper;
import com.gk.study.service.UserThingHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class UserThingHistoryServiceImpl extends ServiceImpl<UserThingHistoryMapper, UserThingHistory>
        implements UserThingHistoryService {

    @Autowired
    private UserThingHistoryMapper userThingHistoryMapper;

    @Override

    public void createHistory(UserThingHistory history) {
        // 如果前端未设置浏览时间，则由后台设置为当前系统时间（格式化字符串）
        if(history.getBrowseTime() == null || history.getBrowseTime().isEmpty()){
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            history.setBrowseTime(currentTime);
        }
        userThingHistoryMapper.insert(history);
    }
}
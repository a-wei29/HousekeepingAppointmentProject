package com.gk.study.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
public interface UserService {
//    List<User> getUserList(Integer role, String mobile);

    IPage<User> getUserList(Integer role, String mobile, Page<User> pageParam);

    User getAdminUser(User user);
    User getNormalUser(User user);
    void createUser(User user);
    void deleteUser(String id);

    void updateUser(User user);

    User getUserByToken(String token);
    User getUserByUserName(String username);

    User getUserDetail(String userId);

    User getUserByWeChatOpenId(String openId);
}

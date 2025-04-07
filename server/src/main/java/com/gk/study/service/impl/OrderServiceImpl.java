package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.Order;
import com.gk.study.entity.Thing;
import com.gk.study.entity.User;
import com.gk.study.mapper.UserMapper;
import com.gk.study.service.OrderService;
import com.gk.study.mapper.OrderMapper;
import com.gk.study.service.ThingService;
import com.gk.study.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Autowired
    OrderMapper orderMapper;

    @Autowired
    UserService userService;

    @Autowired
    ThingService thingService;

    @Autowired
    UserMapper userMapper;

    @Override
    @Transactional
    public void createOrder(Order order) {
        // 初始状态设置为 "0" 待接单
        order.setStatus("0");
        // 订单时间，格式可以根据需求定制
        order.setOrderTime(String.valueOf(System.currentTimeMillis()));
        orderMapper.insert(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderMapper.selectById(orderId);
        if(order != null) {
            order.setStatus(status);
            // 根据状态还可以做其他操作，例如设置 payTime 等
            if("5".equals(status)) { // 用户付款
                order.setPayTime(String.valueOf(System.currentTimeMillis()));
            }
            orderMapper.updateById(order);
        }
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public IPage<Order> listOrders(Page<Order> page) {
        return orderMapper.selectPage(page, null);
    }

    @Override
    public List<Order> getOrderList() {
        return orderMapper.getList();
    }

//    @Override
//    public void createOrder(Order order) {
//        long ct = System.currentTimeMillis();
//        order.setOrderTime(String.valueOf(ct));
//        order.setOrderNumber(String.valueOf(ct));
//        order.setStatus("1");
//        orderMapper.insert(order);
//    }

    @Override
    public void deleteOrder(String id) {
        orderMapper.deleteById(id);
    }

    @Override
    public void updateOrder(Order order) {
        orderMapper.updateById(order);
    }

    @Override
    public List<Order> getUserOrderList(String userId, String status) {
        return orderMapper.getUserOrderList(userId, status);
    }

    @Override
    public IPage<Order> listOrdersByUserId(Long userId, String status, Page<Order> page) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        IPage<Order> resultPage = orderMapper.selectPage(page, queryWrapper);

        // 对查询结果进行补全：补充用户名、服务标题、封面和价格
        if (resultPage.getRecords() != null) {
            for (Order order : resultPage.getRecords()) {
                // 补充用户信息
                try {
                    Long orderUserId = Long.parseLong(order.getUserId());
                    User user = userService.getUserDetail(String.valueOf(orderUserId));
                    if (user != null) {
                        order.setUsername(user.getUsername());
                    }
                } catch (NumberFormatException e) {
                    // 如果转换失败，直接跳过
                }

                // 补充家政服务（商品）信息
                // 假设 thingService.getThingById 接受 String 类型的 id
                Thing thing = thingService.getThingById(order.getThingId());
                if (thing != null) {
                    order.setTitle(thing.getTitle());
                    order.setCover(thing.getCover());
                    order.setPrice(String.valueOf(thing.getPrice()));
                }
            }
        }

        return resultPage;
    }

    @Override
    public IPage<Order> listOrdersByProvider(Long providerUserId, String status, Page<Order> page) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        // 通过子查询查找所有由该服务提供者发布的服务的 id，然后查询对应订单
        queryWrapper.inSql("thing_id", "SELECT id FROM b_thing WHERE user_id = " + providerUserId);
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        IPage<Order> resultPage = orderMapper.selectPage(page, queryWrapper);

        // 同样对结果进行扩充
        if (resultPage.getRecords() != null) {
            for (Order order : resultPage.getRecords()) {
                // 补充用户信息
                try {
                    Long orderUserId = Long.parseLong(order.getUserId());
                    User user = userService.getUserDetail(String.valueOf(orderUserId));
                    if (user != null) {
                        order.setUsername(user.getUsername());
                    }
                } catch (NumberFormatException e) {
                    // 跳过
                }

                // 补充家政服务（商品）信息
                Thing thing = thingService.getThingById(order.getThingId());
                if (thing != null) {
                    order.setTitle(thing.getTitle());
                    order.setCover(thing.getCover());
                    order.setPrice(String.valueOf(thing.getPrice()));
                }
            }
        }

        return resultPage;
    }
}

package com.gk.study.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.Order;

import java.util.List;

public interface OrderService {
    List<Order> getOrderList();
    /**
     * 创建订单，同时设置初始状态为 "0" (待接单)
     */
    void createOrder(Order order);
    void deleteOrder(String id);

    void updateOrder(Order order);

    /**
     * 根据订单ID更新状态
     */
    void updateOrderStatus(Long orderId, String status);

    /**
     * 获取订单详情
     */
    Order getOrderById(Long orderId);
    List<Order> getUserOrderList(String userId, String status);

    /**
     * 分页查询订单列表（可以根据用户、状态、时间等条件扩展）
     */
    IPage<Order> listOrders(Page<Order> page);


    /**
     * 分页查询指定用户下的订单（用户下单的订单）
     */
    IPage<Order> listOrdersByUserId(Long userId, Page<Order> page);

    /**
     * 分页查询服务提供者发布的服务产生的订单
     * 即查询 Order 中 thingId 对应的 Thing 表的 user_id 为 providerUserId 的记录
     */
    IPage<Order> listOrdersByProvider(Long providerUserId, Page<Order> page);
}

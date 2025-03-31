package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gk.study.entity.Order;
import com.gk.study.service.OrderService;
import com.gk.study.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    @Autowired
    OrderMapper orderMapper;


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
    public IPage<Order> listOrdersByUserId(Long userId, Page<Order> page) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return orderMapper.selectPage(page, queryWrapper);
    }

    @Override
    public IPage<Order> listOrdersByProvider(Long providerUserId, Page<Order> page) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        // 通过子查询方式查找出所有由该服务提供者发布的 Thing 的 id，然后查询对应订单
        queryWrapper.inSql("thing_id", "SELECT id FROM b_thing WHERE user_id = " + providerUserId);
        return orderMapper.selectPage(page, queryWrapper);
    }
}

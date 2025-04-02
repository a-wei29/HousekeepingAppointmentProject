package com.gk.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.OrderStatusFlow;
import com.gk.study.mapper.OrderStatusFlowMapper;
import com.gk.study.service.OrderStatusFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusFlowServiceImpl implements OrderStatusFlowService {

    @Autowired
    private OrderStatusFlowMapper flowMapper;

    @Override
    @Transactional
    public void createFlow(OrderStatusFlow flow) {
        flowMapper.insert(flow);
    }

    @Override
    public IPage<OrderStatusFlow> listFlowByOrderId(String orderId, Page<OrderStatusFlow> page) {
        QueryWrapper<OrderStatusFlow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        queryWrapper.orderByAsc("update_time");
        return flowMapper.selectPage(page, queryWrapper);
    }
}
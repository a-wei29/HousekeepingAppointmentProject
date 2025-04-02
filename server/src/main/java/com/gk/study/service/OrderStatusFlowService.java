package com.gk.study.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.entity.OrderStatusFlow;

public interface OrderStatusFlowService {
    /**
     * 创建状态变更记录
     */
    void createFlow(OrderStatusFlow flow);

    /**
     * 根据订单ID查询状态变更记录列表
     */
    IPage<OrderStatusFlow> listFlowByOrderId(String orderId, Page<OrderStatusFlow> page);
}

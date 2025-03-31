package com.gk.study.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Order;
import com.gk.study.enums.OrderStatus;
import com.gk.study.jwt.JwtUtil;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final static Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户创建订单接口
     */
    @Operation(
            summary = "创建订单",
            description = "用户下单后，创建订单，初始状态为待接单（0）",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数不合法")
            }
    )
    @PostMapping("/create")
    @Transactional
    public ResponseEntity<APIResponse<?>> createOrder(
            @RequestBody Order order,
            @RequestHeader("Authorization") String token) {
        // 解析 token 获取当前用户信息（假设 token 中包含 username 等信息）
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String username = jwtUtil.extractUsername(jwtToken);
        // 此处可调用用户服务获取用户信息，进而设置 order.userId
        // order.setUserId(currentUser.getId());

        // 设置初始状态和订单时间
        order.setStatus(OrderStatus.WAITING.getDescription());
        order.setOrderTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 调用 service 层创建订单
        orderService.createOrder(order);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "订单创建成功", order));
    }

    /**
     * 更新订单状态接口
     * 根据订单ID和传入的状态码，更新订单状态。
     */
    @PostMapping("/updateStatus")
    @Transactional
    public ResponseEntity<APIResponse<?>> updateOrderStatus(
            @RequestParam Long orderId,
            @RequestParam String status) {
        int statusCode;
        try {
            statusCode = Integer.parseInt(status);
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "状态码格式不正确"));
        }
        OrderStatus orderStatus = OrderStatus.getByCode(statusCode);
        if (orderStatus == null) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "无效的订单状态"));
        }
        // 获取枚举中对应的描述字符串
        String statusString = orderStatus.getDescription();
        // 此处需要调整 OrderService.updateOrderStatus 的方法签名，接收 String 类型的状态值
        orderService.updateOrderStatus(orderId, statusString);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "订单状态更新成功"));
    }


    /**
     * 查询订单详情
     */
    @Operation(
            summary = "查询订单详情",
            description = "根据订单ID获取订单详细信息",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "订单不存在")
            }
    )
    @GetMapping("/detail")
    public ResponseEntity<APIResponse<?>> getOrderDetail(
            @Parameter(description = "订单ID", required = true) @RequestParam Long orderId) {
        Order order = orderService.getOrderById(orderId);
        if(order == null){
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "订单不存在"));
        }
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", order));
    }

    /**
     * 分页查询订单列表（可扩展为按用户、状态筛选）
     */
    @Operation(
            summary = "查询订单列表",
            description = "分页查询订单列表",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
            }
    )
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> listOrders(
            @Parameter(description = "当前页码", required = true) @RequestParam int page,
            @Parameter(description = "每页记录数", required = true) @RequestParam int size) {
        Page<Order> pageParam = new Page<>(page, size);
        IPage<Order> resultPage = orderService.listOrders(pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
    }


    @GetMapping("/userOrders")
    public ResponseEntity<APIResponse<?>> getUserOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Order> pageParam = new Page<>(page, size);
        IPage<Order> resultPage = orderService.listOrdersByUserId(userId, pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
    }


    @GetMapping("/providerOrders")
    public ResponseEntity<APIResponse<?>> getProviderOrders(
            @RequestParam Long providerUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Order> pageParam = new Page<>(page, size);
        IPage<Order> resultPage = orderService.listOrdersByProvider(providerUserId, pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
    }
}

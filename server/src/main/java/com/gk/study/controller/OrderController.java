package com.gk.study.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Order;
import com.gk.study.entity.OrderStatusFlow;
import com.gk.study.enums.OrderStatus;
import com.gk.study.jwt.JwtUtil;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.requestEntity.UpdateOrderStatusRequest;
import com.gk.study.service.OrderService;
import com.gk.study.service.OrderStatusFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private OrderStatusFlowService orderStatusFlowService;

    @Autowired
    private JwtUtil jwtUtil;

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
        order.setStatus(String.valueOf(OrderStatus.WAITING.getCode()));
        order.setOrderTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 调用 service 层创建订单
        orderService.createOrder(order);

        // 创建订单状态流记录
        OrderStatusFlow flow = new OrderStatusFlow();
        flow.setOrderId(order.getId().toString());
        flow.setStatusCode("0"); // 初始状态码
        flow.setStatus(OrderStatus.WAITING.getDescription());
        flow.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        flow.setOperator(username); // 可记录操作人
        flow.setRemark("订单创建");
        orderStatusFlowService.createFlow(flow);

        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "订单创建成功", order));
    }

    @Operation(
            summary = "更新订单状态接口",
            description = "根据订单ID和传入的状态码更新订单状态，并记录状态变更流。传入状态码对应 OrderStatus 枚举中的 code，系统将转换为对应描述写入订单记录。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "订单状态更新成功"),
            @ApiResponse(responseCode = "400", description = "状态码格式不正确或无效的订单状态")
    })
    @PostMapping("/updateStatus")
    @Transactional
    public ResponseEntity<APIResponse<?>> updateOrderStatus(
            @RequestBody @Parameter(description = "订单状态更新请求体", required = true) UpdateOrderStatusRequest request,
            @Parameter(description = "授权token", required = true) @RequestHeader("Authorization") String token) {

        int statusCode;
        try {
            statusCode = Integer.parseInt(request.getStatus());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "状态码格式不正确"));
        }

        OrderStatus orderStatus = OrderStatus.getByCode(statusCode);
        if (orderStatus == null) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "无效的订单状态"));
        }

        // 获取枚举中对应的描述字符串
        String statusString = orderStatus.getDescription();

        // 更新订单状态（Service 方法修改为接收 String 类型的状态）

        orderService.updateOrderStatus(request.getOrderId(),request.getStatus());

        // 获取操作人信息
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String operator = jwtUtil.extractUsername(jwtToken);

        // 处理 remark 字段：如果前端未传，则使用默认值 "状态更新"
        String remark = (request.getRemark() == null || request.getRemark().trim().isEmpty())
                ? "状态更新" : request.getRemark();

        // 写入状态流记录
        OrderStatusFlow flow = new OrderStatusFlow();
        flow.setOrderId(request.getOrderId().toString());
        flow.setStatusCode(String.valueOf(statusCode));
        flow.setStatus(statusString);
        flow.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        flow.setOperator(operator);
        flow.setRemark(remark);
        orderStatusFlowService.createFlow(flow);

        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "订单状态更新成功"));
    }



    @Operation(
            summary = "查询订单状态变更记录",
            description = "根据订单ID查询该订单的所有状态流记录",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "无记录")
            }
    )
    @GetMapping("/statusFlow")
    public ResponseEntity<APIResponse<?>> getOrderStatusFlow(
            @RequestParam Long orderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderStatusFlow> pageParam = new Page<>(page, size);
        IPage<OrderStatusFlow> resultPage = orderStatusFlowService.listFlowByOrderId(orderId.toString(), pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
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


    @Operation(
            summary = "查询用户订单",
            description = "根据用户ID查询该用户下单的所有订单，并支持分页查询。可选的筛选条件 status 表示订单状态，若传入，则只返回状态匹配的订单。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "未找到订单记录")
    })
    @GetMapping("/userOrders")
    public ResponseEntity<APIResponse<?>> getUserOrders(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "订单状态筛选", required = false) @RequestParam(required = false) String status,
            @Parameter(description = "当前页码", required = false) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数", required = false) @RequestParam(defaultValue = "10") int size) {

        Page<Order> pageParam = new Page<>(page, size);
        // 调用 service 层新增的带状态筛选的方法
        IPage<Order> resultPage = orderService.listOrdersByUserId(userId, status, pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
    }

    @Operation(
            summary = "查询服务提供者订单",
            description = "根据服务提供者ID查询其发布的服务被下的订单，并支持分页查询。可选的筛选条件 status 表示订单状态，若传入，则只返回状态匹配的订单。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "未找到订单记录")
    })
    @GetMapping("/providerOrders")
    public ResponseEntity<APIResponse<?>> getProviderOrders(
            @Parameter(description = "服务提供者ID", required = true) @RequestParam Long providerUserId,
            @Parameter(description = "订单状态筛选", required = false) @RequestParam(required = false) String status,
            @Parameter(description = "当前页码", required = false) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数", required = false) @RequestParam(defaultValue = "10") int size) {

        Page<Order> pageParam = new Page<>(page, size);
        // 调用 service 层新增的带状态筛选的方法
        IPage<Order> resultPage = orderService.listOrdersByProvider(providerUserId, status, pageParam);
        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage));
    }
}

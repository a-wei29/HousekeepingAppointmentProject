package com.gk.study.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.*;
import com.gk.study.jwt.JwtUtil;
import com.gk.study.permission.Access;
import com.gk.study.permission.AccessLevel;
import com.gk.study.requestEntity.*;
import com.gk.study.service.UserService;
import com.gk.study.enums.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/userAuth")
public class UserAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${File.uploadPath}")
    private String uploadPath;

    @Operation(
            summary = "获取用户列表",
            description = "查询用户列表，可以根据角色和手机号进行模糊搜索。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "400", description = "查询参数不合法")
            }
    )
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> list( @RequestBody(required = false) UserListRequest request) {
        logger.info("调用 /list 接口, role: {}, mobile: {}, page: {}, size: {}",
                request.getRole(), request.getMobile(), request.getPage(), request.getSize());
        if (request == null) {
            request = new UserListRequest();  // 会使用 page=1, size=10 的默认值
        }
        // 构造分页对象
        Page<User> pageParam = new Page<>(request.getPage(), request.getSize());
        IPage<User> resultPage = userService.getUserList(request.getRole(), request.getMobile(), pageParam);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", resultPage)
        );
    }

    @Operation(
            summary = "获取用户详情",
            description = "根据用户ID查询用户详情。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "404", description = "用户不存在")
            }
    )
    @GetMapping("/detail")
    public ResponseEntity<APIResponse<?>> detail(@Parameter(description = "用户ID", required = true)  @RequestParam String userId){
        User user = userService.getUserDetail(userId);
        if(user == null){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", user)
        );
    }

    // ================== 用户注册（普通 / 微信 / 其他） ==================

    /**
     * 普通用户注册 (结合了之前 userRegister + handleNormalRegister 的逻辑)
     */
    @Operation(
            summary = "用户注册",
            description = "支持普通注册和微信注册。根据提供的微信代码或普通注册信息进行用户注册。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "注册成功"),
                    @ApiResponse(responseCode = "400", description = "注册失败")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<APIResponse<?>> register(@Parameter(description = "注册请求数据", required = true) @RequestBody RegisterRequest request) throws IOException {
        // 如果有 wechatCode，就优先走微信注册，否则走普通注册
        if (!StringUtils.isEmpty(request.getWechatCode())) {
            return ResponseEntity.ok(handleWeChatRegister(request));
        } else {
            return ResponseEntity.ok(handleNormalRegister(request));
        }
    }

    /**
     * [A] 普通注册逻辑
     */
    private APIResponse<?> handleNormalRegister(RegisterRequest request) {
        logger.info("handleNormalRegister : {}", request);
        // 1. 校验必填项
        if (StringUtils.isEmpty(request.getUsername()) ||
                StringUtils.isEmpty(request.getPassword()) ||
                StringUtils.isEmpty(request.getRePassword())) {
            return new APIResponse<>(ResponeCode.FAIL, "缺少必要字段");
        }
        // 2. 校验用户名是否重复
        User userInDb = userService.getUserByUserName(request.getUsername());
        if (userInDb != null) {
            return new APIResponse<>(ResponeCode.FAIL, "用户名已被占用");
        }
        // 3. 验证两次密码是否一致
        if (!request.getPassword().equals(request.getRePassword())) {
            return new APIResponse<>(ResponeCode.FAIL, "两次输入的密码不一致");
        }
        // 4. 使用BCrypt加密
        String bcryptPwd = passwordEncoder.encode(request.getPassword());

        // 5. 组装User对象
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(bcryptPwd);
        // 这里设定普通用户角色
        newUser.setRole(UserRole.NORMAL_USER.getCode());
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));
        newUser.setMobile(request.getPhone());
        newUser.setEmail(request.getEmail());
        // 生成一个 token (可选)
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        userService.createUser(newUser);
        return new APIResponse<>(ResponeCode.SUCCESS, "普通用户注册成功", newUser);
    }

    /**
     * [B] 微信注册逻辑
     */
    private APIResponse<?> handleWeChatRegister(RegisterRequest request) throws IOException {
        // 1. 根据wechatCode换取openid
        String openid = getWeChatOpenId(request.getWechatCode());
        // 2. 查数据库，看是否已经绑定
        User existUser = userService.getUserByWeChatOpenId(openid);
        if(existUser != null){
            return new APIResponse<>(ResponeCode.FAIL, "该微信已绑定过账号，请直接登录");
        }
        // 3. 自动注册
        User newUser = new User();
        String autoUsername = "wx_" + System.currentTimeMillis();
        newUser.setUsername(autoUsername);
        // 可以设置随机密码
        newUser.setPassword(passwordEncoder.encode("WX_" + UUID.randomUUID()));

        newUser.setWechatOpenid(openid);
        newUser.setRole(User.NormalUser);
        newUser.setStatus("0");
        newUser.setCreateTime(String.valueOf(System.currentTimeMillis()));
        newUser.setToken(UUID.randomUUID().toString().replaceAll("-", ""));
        // 其他信息 ...
        userService.createUser(newUser);

        return new APIResponse<>(ResponeCode.SUCCESS, "微信注册成功", newUser);
    }

    private String getWeChatOpenId(String code) throws IOException {
        // TODO: 与微信服务器交互获取 openid
        // 示例直接返回一个假 openid
        return "fake_openid_" + code;
    }

    // ================== 登录接口（来自原 AuthController） ==================

    /**
     * 用户名+密码登录
     */
    @Operation(
            summary = "用户名和密码登录",
            description = "通过用户名和密码进行登录，成功后返回JWT令牌。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "401", description = "用户名或密码错误")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<APIResponse<?>> login(@RequestBody LoginRequest request) {
        // 1. 构造用户名密码登录token
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        // 2. 执行认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException e) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名或密码错误")
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "登录失败: " + e.getMessage())
            );
        }

        // 3. 若认证成功, 生成JWT并返回
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(request.getUsername());
        User user = userService.getUserByUserName(request.getUsername());
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", request.getUsername());
        dataMap.put("userId",user.getId());
        dataMap.put("message", "登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "登录成功", dataMap)
        );
    }

    /**
     * 手机号验证码登录
     */
    @Operation(
            summary = "手机号验证码登录",
            description = "通过手机号和验证码登录，成功后返回JWT令牌。(暂时未完成)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "验证码错误")
            }
    )
    @PostMapping("/loginByPhone")
    public ResponseEntity<APIResponse<?>> loginByPhone(@Parameter(description = "手机号登录请求数据", required = true) @RequestBody PhoneLoginRequest request) {
        boolean pass = checkSmsCode(request.getPhone(), request.getSmsCode());
        if (!pass) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "验证码错误")
            );
        }
        // DB查询或自动注册
        String username = loadUsernameByPhone(request.getPhone());
        String jwt = jwtUtil.generateToken(username);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("phone", request.getPhone());
        dataMap.put("message", "手机登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "手机登录成功", dataMap)
        );
    }

    /**
     * 微信登录
     */
    @Operation(
            summary = "微信登录",
            description = "通过微信登录，提供微信code获取openId，若用户不存在则自动注册。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "微信登录失败")
            }
    )
    @PostMapping("/loginByWeChat")
    public ResponseEntity<APIResponse<?>> loginByWeChat(@Parameter(description = "微信登录请求数据", required = true)  @RequestBody WeChatLoginRequest request) throws IOException {
        String openId;
        openId = getWeChatOpenId(request.getCode());
        // 若不存在则自动注册
        String username = loadOrCreateUserByWeChatOpenId(openId);
        String jwt = jwtUtil.generateToken(username);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("token", jwt);
        dataMap.put("username", username);
        dataMap.put("message", "微信登录成功");

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "微信登录成功", dataMap)
        );
    }

    private boolean checkSmsCode(String phone, String smsCode) {
        // TODO: 调用你的短信服务检查
        return true;
    }
    private String loadUsernameByPhone(String phone) {
        // TODO: 查数据库, 若无则自动新建
        return "userOf_" + phone;
    }
    private String loadOrCreateUserByWeChatOpenId(String openid) {
        // TODO: 查数据库, 若不存在则注册
        return "userOf_" + openid;
    }

    // ================== 其他用户管理接口（后台 / 普通） ==================

    @Access(level = AccessLevel.ADMIN)
    @PostMapping("/create")
    @Transactional
    @Operation(
            summary = "管理员创建用户",
            description = "管理员通过接口创建用户，创建过程中会进行用户名校验。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "创建成功"),
                    @ApiResponse(responseCode = "400", description = "创建失败")
            }
    )
    public ResponseEntity<APIResponse<?>> create(@Parameter(description = "用户信息", required = true) @RequestBody User user) throws IOException {
        // 管理员创建用户
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名或密码不能为空")
            );
        }
        // 查重
        if(userService.getUserByUserName(user.getUsername()) != null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户名重复")
            );
        }
        String bcryptPwd = passwordEncoder.encode(user.getPassword());
        user.setPassword(bcryptPwd);
        user.setCreateTime(String.valueOf(System.currentTimeMillis()));
        user.setToken(UUID.randomUUID().toString().replaceAll("-", ""));

        // 这里可以处理头像
        // ...

        userService.createUser(user);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "创建成功")
        );
    }

    @Access(level = AccessLevel.ADMIN)
    @Operation(
            summary = "删除用户",
            description = "管理员删除指定用户。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功"),
                    @ApiResponse(responseCode = "404", description = "用户不存在")
            }
    )
    @PostMapping("/delete")
    public ResponseEntity<APIResponse<?>> delete(@Parameter(description = "用户ID列表", required = true) @RequestParam String ids){
        String[] arr = ids.split(",");
        for (String id : arr) {
            userService.deleteUser(id);
        }
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "删除成功")
        );
    }

    @Access(level = AccessLevel.ADMIN)
    @Operation(
            summary = "更新用户信息",
            description = "管理员更新用户信息，密码不允许直接修改。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "更新失败")
            }
    )
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<APIResponse<?>> update(@Parameter(description = "用户信息", required = true) @RequestBody User user) throws IOException {
        // 不允许直接改密码
        user.setPassword(null);
        // 处理头像
        // ...
        userService.updateUser(user);
        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
        );
    }

    @Access(level = AccessLevel.LOGIN)
    @Operation(
            summary = "更新用户信息",
            description = "用户更新自己的个人信息，不允许修改密码。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "更新失败")
            }
    )
    @PostMapping("/updateUserInfo")
    @Transactional
    public ResponseEntity<APIResponse<?>> updateUserInfo(
            @Parameter(description = "用户信息", required = true)
            @RequestBody User user) throws IOException {

        // 1. 查询原始用户
        User existing = userService.getUserDetail(String.valueOf(user.getId()));
        if (existing == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }

        // 2. 保留原密码，不允许更新
        user.setPassword(existing.getPassword());

        // 3. 保留原角色，不作修改
        user.setRole(existing.getRole());

        // 4. 处理头像文件上传（如果前端传了 avatarFile）
        if (user.getAvatarFile() != null && !user.getAvatarFile().isEmpty()) {
            byte[] avatarBytes = user.getAvatarFile().getBytes();
            user.setAvatar(avatarBytes);
        } else {
            // 如果没有传新头像，则保留老头像
            user.setAvatar(existing.getAvatar());
        }

        // 5. 调用 Service 执行更新
        userService.updateUser(user);

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
        );
    }

    @Access(level = AccessLevel.LOGIN)
    @Operation(
            summary = "更新用户密码",
            description = "普通用户修改自己的密码，要求提供原密码和新密码。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功"),
                    @ApiResponse(responseCode = "400", description = "密码修改失败")
            }
    )
    @PostMapping("/updatePwd")
    @Transactional
    public ResponseEntity<APIResponse<?>> updatePwd(@Parameter(description = "用户ID", required = true) @RequestParam String userId,
                                                    @Parameter(description = "原密码", required = true) @RequestParam String password,
                                                    @Parameter(description = "新密码", required = true) @RequestParam String newPassword) throws IOException {
        User user = userService.getUserDetail(userId);
        if(user == null){
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }
            // 校验旧密码
            if(!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.ok(
                        new APIResponse<>(ResponeCode.FAIL, "原密码错误")
                );
            }
            // 更新为新密码
            String bcryptPwd = passwordEncoder.encode(newPassword);
            user.setPassword(bcryptPwd);
            userService.updateUser(user);
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.SUCCESS, "更新成功")
            );
    }

    @Operation(
            summary = "获取当前登录的用户信息",
            description = "根据JWT令牌获取当前登录的用户信息。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功"),
                    @ApiResponse(responseCode = "401", description = "未授权")
            }
    )
    @GetMapping("/currentUser")
    public ResponseEntity<APIResponse<?>> getCurrentUserInfo(@RequestHeader("Authorization") String token) {
        // 1. 从请求头中提取 token
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // 2. 解析 token，获取用户名
        String username = jwtUtil.extractUsername(jwtToken);

        // 3. 根据用户名查询用户信息
        User user = userService.getUserByUserName(username);

        // 4. 返回用户信息
        if (user == null) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "用户不存在")
            );
        }

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "查询成功", user)
        );
    }


    @Operation(summary = "上传用户头像", description = "接收 userId 和头像文件，将图片二进制存入数据库，并同步到阿里云 OSS。")
    @Access(level = AccessLevel.LOGIN)
    @PostMapping("/uploadAvatar")
    public ResponseEntity<APIResponse<?>> uploadAvatar(
            @RequestParam("userId") Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        // 1. 参数校验
        if (userId == null || file == null || file.isEmpty()) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "参数不合法"));
        }
        User user = userService.getUserDetail(String.valueOf(userId));
        if (user == null) {
            return ResponseEntity.ok(new APIResponse<>(ResponeCode.FAIL, "用户不存在"));
        }

        // 2. 读取文件二进制
        byte[] data = file.getBytes();
        user.setAvatar(data);

//        // 3. 可选：同步到阿里云 OSS（伪代码）
//    /*
//    // —— 阿里云 OSS 集成示例 ——
//    String endpoint        = "https://oss-cn-hangzhou.aliyuncs.com";
//    String accessKeyId     = "<your-access-key-id>";
//    String accessKeySecret = "<your-access-key-secret>";
//    String bucketName      = "myapp-avatars";
//    String objectKey       = "avatars/" + userId + ".jpg";
//
//    // 创建 OSS 客户端
//    OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//    // 上传到 OSS
//    ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(data));
//    // 关闭客户端
//    ossClient.shutdown();
//
//    // 如果你需要在数据库中同时存 OSS 地址，可以：
//    // String avatarUrl = "https://" + bucketName + "." + endpoint + "/" + objectKey;
//    // user.setAvatarUrl(avatarUrl);
//    */

        // 4. 更新数据库
        userService.updateUser(user);

        return ResponseEntity.ok(new APIResponse<>(ResponeCode.SUCCESS, "头像上传成功"));
    }
}


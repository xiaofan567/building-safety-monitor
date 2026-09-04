package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "登录认证", description = "系统登录、登出、获取用户信息")
@RestController
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return Result.ok(adminUserService.login(username, password));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(AuthConst.TOKEN_NAME) String token) {
        adminUserService.logout(token);
        return Result.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @PostMapping("/queryLoginUser")
    public Result<AdminUser> queryLoginUser(@RequestHeader(AuthConst.TOKEN_NAME) String token) {
        return Result.ok(adminUserService.queryLoginUser(token));
    }
}
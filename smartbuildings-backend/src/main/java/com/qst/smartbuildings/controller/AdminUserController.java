package com.qst.smartbuildings.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.PageEntity;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "系统后台用户增删改查、启用禁用、密码重置")
@RestController
@RequestMapping("/api/system/user")
public class AdminUserController {
//系统管理模块接口设计—用户管理接口
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户列表查询")
    @GetMapping("/list")
    public Result<Page<AdminUser>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "账号") @RequestParam(required = false) String username,
            @Parameter(description = "姓名") @RequestParam(required = false) String nickName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPage(pageNum);
        pageEntity.setLimit(pageSize);
        return Result.ok(adminUserService.page(pageEntity, username, nickName, status));
    }

    @Operation(summary = "用户详情查询")
    @GetMapping("/{userId}")
    public Result<AdminUser> getById(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.ok(adminUserService.getById(userId));
    }

    @Operation(summary = "新增系统用户")
    @PostMapping("/add")
    public Result<?> add(@RequestBody AdminUser adminUser) {
        adminUserService.save(adminUser);
        return Result.ok();
    }

    @Operation(summary = "编辑用户信息")
    @PutMapping("/update")
    public Result<?> update(@RequestBody AdminUser adminUser) {
        adminUserService.updateById(adminUser);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{userId}")
    public Result<?> delete(@Parameter(description = "用户ID") @PathVariable Long userId) {
        adminUserService.removeById(userId);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/status")
    public Result<?> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        adminUserService.updateStatus(userId, status);
        return Result.ok();
    }

    @Operation(summary = "密码重置")
    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = params.get("newPassword").toString();
        adminUserService.updatePassword(userId, newPassword);
        return Result.ok();
    }
}
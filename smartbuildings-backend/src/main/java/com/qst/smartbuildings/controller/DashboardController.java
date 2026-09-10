package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页数据看板接口
 *
 * @author 智能建造团队
 */
@Tag(name = "首页数据看板", description = "首页各模块数据聚合（喷淋记录、环境实时数据、危大工程设备统计、报警记录等）")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "首页数据看板")
    @PostMapping("/getDashboardData")
    public Result<Map<String, Object>> getDashboardData() {
        return Result.ok(dashboardService.getDashboardData());
    }
}
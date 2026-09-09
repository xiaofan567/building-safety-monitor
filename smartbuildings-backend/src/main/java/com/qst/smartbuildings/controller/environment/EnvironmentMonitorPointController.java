package com.qst.smartbuildings.controller.environment;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.environment.EnvironmentMonitorPoint;
import com.qst.smartbuildings.service.EnvironmentMonitorPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/environmentMonitorPoint")
@Tag(name = "环境监测点管理",description = "环境监测点的查询、保存、删除等操作接口")
public class EnvironmentMonitorPointController {
    @Autowired
    private EnvironmentMonitorPointService environmentMonitorPointService;

    @PostMapping("/list")
    @Operation(summary = "查询环境监测点", description = "查询所有环境监测点")
    public Result<List<EnvironmentMonitorPoint>> list() {
        return Result.ok(environmentMonitorPointService.list());
    }

    @PostMapping("/update")
    @Operation(summary = "更新环境监测点", description = "根据环境监测点ID更新环境监测点")
    public Result update(@RequestBody EnvironmentMonitorPoint environmentMonitorPoint) {
        boolean b = environmentMonitorPointService.updateById(environmentMonitorPoint);
        return b ? Result.ok() : Result.error(-1, "更新失败");
    }
}

package com.qst.smartbuildings.controller.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.environment.EnvironmentValue;
import com.qst.smartbuildings.entity.environment.param.EnvironmentValueParam;
import com.qst.smartbuildings.service.EnvironmentValueService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/environmentValue")
@Schema(name = "环境值管理",description = "环境值的查询、保存、删除等操作接口")
public class EnvironmentValueController {
    //    1.查询绿色施工环境数据，包括
//     a)实时温湿度，风速，噪声，TSP，PM10，PM2.5
//     b)24小时内的空气质量（pm10，pm2.5），噪声，
//     c)近一周温度（最高温度，最低温度）
//     d)近一周的空气质量平均值（pm2.5）
//     e)历史空气质量分析0<优<35<良<75<轻度污染<115<重度污染<150<重度污染<250<严重污染
//     f)喷淋启停记录 //todo
    @Autowired
    private EnvironmentValueService environmentValueService;
    @PostMapping("/getEnvironmentInfo")
    @Schema(name = "获取环境信息",description = "获取环境信息")
    public Result<Map<String,Object>> getEnvironmentInfo() {
        return Result.ok(environmentValueService.getEnvironmentInfo());
    }
    @PostMapping("/list")
    @Schema(name = "获取环境值列表",description = "获取环境值列表")
    public Result<BPage<EnvironmentValue>> list(@RequestBody EnvironmentValueParam environmentValueParam) {
        return Result.ok(environmentValueService.selectByPage(environmentValueParam));
    }
}

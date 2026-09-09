package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.HfDfpAlarmInfoVO;
import com.qst.smartbuildings.entity.MonitorValue;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.HfAndDfpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 高支模/深基坑监测接口
 *
 * @author 智能建造团队
 */
@Tag(name = "高支模与深基坑监测", description = "高支模、深基坑的列表、监测数据与报警统计")
@RestController
@RequestMapping("/hfAndDfp")
public class HfAndDfpController {

    private final HfAndDfpService hfAndDfpService;

    public HfAndDfpController(HfAndDfpService hfAndDfpService) {
        this.hfAndDfpService = hfAndDfpService;
    }

    @Operation(summary = "查询高支模列表")
    @PostMapping("/hfList")
    public Result<List<EquipmentAssets>> hfList() {
        return Result.ok(hfAndDfpService.hfList());
    }

    @Operation(summary = "查询深基坑")
    @PostMapping("/dfpList")
    public Result<List<EquipmentAssets>> dfpList() {
        return Result.ok(hfAndDfpService.dfpList());
    }

    @Operation(summary = "查询高支模&&深基坑监测数据")
    @PostMapping("/monitor/{assetId}")
    public Result<Map<String, List<MonitorValue>>> monitor(@PathVariable("assetId") Long assetId) {
        return Result.ok(hfAndDfpService.getMonitorData(assetId));
    }

    @Operation(summary = "查询高支模&&深基坑报警数据")
    @PostMapping("/alarm/{assetId}")
    public Result<HfDfpAlarmInfoVO> alarm(@PathVariable("assetId") Long assetId) {
        return Result.ok(hfAndDfpService.getAlarmInfo(assetId));
    }
}

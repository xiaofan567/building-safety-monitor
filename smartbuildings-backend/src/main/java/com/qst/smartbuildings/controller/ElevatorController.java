package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.DeviceDataPageDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.ElevatorAlarmInfoVO;
import com.qst.smartbuildings.entity.ElevatorData;
import com.qst.smartbuildings.entity.ElevatorInfo;
import com.qst.smartbuildings.entity.ElevatorWorkInfo;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.ElevatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 升降机监测接口
 *
 * @author 智能建造团队
 */
@Tag(name = "升降机监测", description = "升降机（施工升降电梯）的列表、实时数据、历史数据、工作记录与报警统计")
@RestController
@RequestMapping("/elevator")
public class ElevatorController {

    private final ElevatorService elevatorService;

    public ElevatorController(ElevatorService elevatorService) {
        this.elevatorService = elevatorService;
    }

    @Operation(summary = "查询升降机列表")
    @PostMapping("/selectAllElevator")
    public Result<List<ElevatorInfo>> selectAllElevator() {
        return Result.ok(elevatorService.selectAllElevator());
    }

    @Operation(summary = "查询升降机检测历史记录")
    @PostMapping("/dataList")
    public Result<BPage<ElevatorData>> dataList(@RequestBody DeviceDataPageDTO dto) {
        return Result.ok(elevatorService.elevatorDataList(dto.getAssetId(), page(dto), limit(dto)));
    }

    @Operation(summary = "查询工作循环记录列表")
    @PostMapping("/workList")
    public Result<BPage<ElevatorWorkInfo>> workList(@RequestBody DeviceDataPageDTO dto) {
        return Result.ok(elevatorService.elevatorWorkList(dto.getAssetId(), page(dto), limit(dto)));
    }

    @Operation(summary = "获取升降机实时数据（id为assetId）")
    @PostMapping("/getData/{assetId}")
    public Result<List<ElevatorData>> getData(@PathVariable("assetId") Long assetId) {
        return Result.ok(elevatorService.getElevatorData(assetId));
    }

    @Operation(summary = "获取升降机报警信息（id为assetId）")
    @PostMapping("/getElevatorAlarmInfo/{assetId}")
    public Result<ElevatorAlarmInfoVO> getElevatorAlarmInfo(@PathVariable("assetId") Long assetId) {
        return Result.ok(elevatorService.getElevatorAlarmInfo(assetId));
    }

    /**
     * 页码，空值默认1
     */
    private long page(DeviceDataPageDTO dto) {
        return dto.getPage() == null || dto.getPage() <= 0 ? 1L : dto.getPage();
    }

    /**
     * 每页数量，空值默认10
     */
    private long limit(DeviceDataPageDTO dto) {
        return dto.getLimit() == null || dto.getLimit() <= 0 ? 10L : dto.getLimit();
    }
}
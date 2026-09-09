package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.DeviceDataPageDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.TowerAlarmInfoVO;
import com.qst.smartbuildings.entity.TowerData;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.TowerWorkInfo;
import com.qst.smartbuildings.entity.TowerWorkInfoVO;
import com.qst.smartbuildings.service.TowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 塔吊（塔机）监测接口
 *
 * @author 智能建造团队
 */
@Tag(name = "塔吊监测", description = "塔吊（塔机）的列表、实时数据、历史数据、工作记录与报警统计")
@RestController
@RequestMapping("/tower")
public class TowerController {

    private final TowerService towerService;

    public TowerController(TowerService towerService) {
        this.towerService = towerService;
    }

    @Operation(summary = "查询塔吊列表")
    @PostMapping("/selectAllTower")
    public Result<List<TowerInfo>> selectAllTower() {
        return Result.ok(towerService.selectAllTower());
    }

    @Operation(summary = "查询塔机监测历史记录")
    @PostMapping("/dataList")
    public Result<BPage<TowerData>> dataList(@RequestBody DeviceDataPageDTO dto) {
        return Result.ok(towerService.towerDataList(dto.getAssetId(), page(dto), limit(dto)));
    }

    @Operation(summary = "查询工作循环记录列表")
    @PostMapping("/workList")
    public Result<BPage<TowerWorkInfo>> workList(@RequestBody DeviceDataPageDTO dto) {
        return Result.ok(towerService.towerWorkList(dto.getAssetId(), page(dto), limit(dto)));
    }

    @Operation(summary = "查询塔吊实时数据 查询十条（id为assetId）")
    @PostMapping("/getData/{assetId}")
    public Result<List<TowerData>> getData(@PathVariable("assetId") Long assetId) {
        return Result.ok(towerService.getTowerData(assetId));
    }

    @Operation(summary = "查询塔吊今日工作状态（id为assetId）")
    @PostMapping("/getTowerWorkInfo/{assetId}")
    public Result<TowerWorkInfoVO> getTowerWorkInfo(@PathVariable("assetId") Long assetId) {
        return Result.ok(towerService.getTowerWorkInfo(assetId));
    }

    @Operation(summary = "查询塔吊报警记录（id为assetId）")
    @PostMapping("/getTowerAlarmInfo/{assetId}")
    public Result<TowerAlarmInfoVO> getTowerAlarmInfo(@PathVariable("assetId") Long assetId) {
        return Result.ok(towerService.getTowerAlarmInfo(assetId));
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
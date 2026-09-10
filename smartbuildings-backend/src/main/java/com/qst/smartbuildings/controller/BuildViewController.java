package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.BuildViewService;
import com.qst.smartbuildings.vo.EquipmentOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机械设备总览接口
 *
 * <p>根据设备类型ID查询机械设备总览（塔吊：145，升降机：115，高支模：147，深基坑：148）。</p>
 *
 * @author 智能建造团队
 */
@Tag(name = "机械设备总览", description = "根据设备类型ID查询机械设备总览")
@RestController
@RequestMapping("/buildView")
public class BuildViewController {

    private final BuildViewService buildViewService;

    public BuildViewController(BuildViewService buildViewService) {
        this.buildViewService = buildViewService;
    }

    @Operation(summary = "根据设备类型ID查询机械设备总览")
    @PostMapping("/overview")
    public Result<EquipmentOverviewVO> overview(@RequestParam("id") Long id) {
        return Result.ok(buildViewService.overview(id));
    }
}
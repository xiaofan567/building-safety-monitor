package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.EquipmentLocationDeleteDTO;
import com.qst.smartbuildings.entity.EquipmentLocation;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.EquipmentLocationService;
import com.qst.smartbuildings.vo.EquipmentLocationTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备位置（地址）管理接口
 *
 * @author 智能建造团队
 */
@Tag(name = "设备地址管理", description = "设备地址树查询、详情、新增修改、删除")
@RestController
@RequestMapping("/equipmentLocation")
public class EquipmentLocationController {

    private final EquipmentLocationService equipmentLocationService;

    public EquipmentLocationController(EquipmentLocationService equipmentLocationService) {
        this.equipmentLocationService = equipmentLocationService;
    }

    @Operation(summary = "地址树查询")
    @PostMapping("/queryTreeList")
    public Result<List<EquipmentLocationTreeVO>> queryTreeList() {
        return Result.ok(equipmentLocationService.queryTreeList());
    }

    @Operation(summary = "地址详情查询")
    @PostMapping("/get/{locationId}")
    public Result<EquipmentLocation> get(@PathVariable("locationId") Long locationId) {
        return Result.ok(equipmentLocationService.getById(locationId));
    }

    @Operation(summary = "新增或修改地址")
    @PostMapping("/save")
    public Result<?> save(@Valid @RequestBody EquipmentLocation equipmentLocation) {
        equipmentLocationService.saveLocation(equipmentLocation);
        return Result.ok();
    }

    @Operation(summary = "删除地址")
    @PostMapping("/delete")
    public Result<?> delete(@Valid @RequestBody EquipmentLocationDeleteDTO dto) {
        equipmentLocationService.delete(dto.getLocationId());
        return Result.ok();
    }
}
package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.EquipmentAssetsDeleteDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.EquipmentAssetsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 设备管理接口
 *
 * @author 智能建造团队
 */
@Tag(name = "设备管理", description = "设备列表、详情、新增修改、删除")
@RestController
@RequestMapping("/equipmentAssets")
public class EquipmentAssetsController {

    private final EquipmentAssetsService equipmentAssetsService;

    public EquipmentAssetsController(EquipmentAssetsService equipmentAssetsService) {
        this.equipmentAssetsService = equipmentAssetsService;
    }

    @Operation(summary = "查询可选择的设备列表（状态为启用）")
    @PostMapping("/list")
    public Result<BPage<EquipmentAssets>> list(@RequestBody(required = false) Map<String, Object> params) {
        long current = numberValue(params, "current", 1L);
        long size = numberValue(params, "size", 10L);
        return Result.ok(equipmentAssetsService.pageList(current, size));
    }

    @Operation(summary = "查询设备详情")
    @PostMapping("/get/{assetId}")
    public Result<EquipmentAssets> get(@PathVariable("assetId") Long assetId) {
        return Result.ok(equipmentAssetsService.getById(assetId));
    }

    @Operation(summary = "保存或修改设备")
    @PostMapping("/save")
    public Result<?> save(@Valid @RequestBody EquipmentAssets equipmentAssets) {
        equipmentAssetsService.saveAssets(equipmentAssets);
        return Result.ok();
    }

    @Operation(summary = "删除设备")
    @PostMapping("/delete")
    public Result<?> delete(@Valid @RequestBody EquipmentAssetsDeleteDTO dto) {
        equipmentAssetsService.delete(dto.getAssetId());
        return Result.ok();
    }

    /**
     * 从请求参数中读取数字（兼容数字与字符串），为空时返回默认值
     */
    private long numberValue(Map<String, Object> params, String key, long defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

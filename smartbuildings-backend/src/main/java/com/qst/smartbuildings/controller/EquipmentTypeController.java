package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.EquipmentTypeDeleteDTO;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.EquipmentTypeService;
import com.qst.smartbuildings.vo.EquipmentTypeTreeVO;
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
 * 设备类型管理接口
 *
 * @author 智能建造团队
 */
@Tag(name = "设备类型管理", description = "设备类型树查询、详情、新增修改、删除")
@RestController
@RequestMapping("/equipmentType")
public class EquipmentTypeController {

    private final EquipmentTypeService equipmentTypeService;

    public EquipmentTypeController(EquipmentTypeService equipmentTypeService) {
        this.equipmentTypeService = equipmentTypeService;
    }

    @Operation(summary = "类型树查询")
    @PostMapping("/queryTreeList")
    public Result<List<EquipmentTypeTreeVO>> queryTreeList() {
        return Result.ok(equipmentTypeService.queryTreeList());
    }

    @Operation(summary = "设备类型详情查询")
    @PostMapping("/get/{typeId}")
    public Result<EquipmentType> get(@PathVariable("typeId") Long typeId) {
        return Result.ok(equipmentTypeService.getById(typeId));
    }

    @Operation(summary = "新增或修改设备类型")
    @PostMapping("/save")
    public Result<?> save(@Valid @RequestBody EquipmentType equipmentType) {
        equipmentTypeService.saveType(equipmentType);
        return Result.ok();
    }

    @Operation(summary = "删除设备类型")
    @PostMapping("/delete")
    public Result<?> delete(@Valid @RequestBody EquipmentTypeDeleteDTO dto) {
        equipmentTypeService.delete(dto.getTypeId());
        return Result.ok();
    }
}
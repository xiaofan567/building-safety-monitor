package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.common.UpdateOperationEnum;
import com.qst.smartbuildings.dto.EquipmentMonitorPointDeleteDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.EquipmentMonitorPointService;
import com.qst.smartbuildings.tcpserver.handle.EquipmentAssetsDataProcessHandle;
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
 * 监测点管理接口
 *
 * @author 智能建造团队
 */
@Tag(name = "监测点管理", description = "监测点列表、详情、新增更新、删除")
@RestController
@RequestMapping("/equipmentMonitorPoint")
public class EquipmentMonitorPointController {

    private final EquipmentMonitorPointService equipmentMonitorPointService;
    private final EquipmentAssetsDataProcessHandle dataProcessHandle;

    public EquipmentMonitorPointController(EquipmentMonitorPointService equipmentMonitorPointService,
                                           EquipmentAssetsDataProcessHandle dataProcessHandle) {
        this.equipmentMonitorPointService = equipmentMonitorPointService;
        this.dataProcessHandle = dataProcessHandle;
    }

    @Operation(summary = "查询监测点列表")
    @PostMapping("/list")
    public Result<BPage<EquipmentMonitorPoint>> list(@RequestBody(required = false) Map<String, Object> params) {
        long page = numberValue(params, "page", 1L);
        long limit = numberValue(params, "limit", 10L);
        return Result.ok(equipmentMonitorPointService.pageList(page, limit));
    }

    @Operation(summary = "查询监测点详情")
    @PostMapping("/getById/{id}")
    public Result<EquipmentMonitorPoint> getById(@PathVariable("id") Long id) {
        return Result.ok(equipmentMonitorPointService.getById(id));
    }

    @Operation(summary = "新增或更新监测点信息")
    @PostMapping("/saveOrUpdate")
    public Result<?> saveOrUpdate(@RequestBody EquipmentMonitorPoint equipmentMonitorPoint) {
        equipmentMonitorPointService.savePoint(equipmentMonitorPoint);
        // 同步监测点数据到TCP数据处理组件（内部根据状态自动判断新增/更新/删除）
        dataProcessHandle.updateEquipmentMonitorPoint(equipmentMonitorPoint, UpdateOperationEnum.UPDATE);
        return Result.ok();
    }

    @Operation(summary = "删除监测点")
    @PostMapping("/delete")
    public Result<?> delete(@Valid @RequestBody EquipmentMonitorPointDeleteDTO dto) {
        // 删除前先查询出监测点信息，用于同步删除TCP服务中的监测点缓存
        EquipmentMonitorPoint point = equipmentMonitorPointService.getById(dto.getId());
        equipmentMonitorPointService.delete(dto.getId());
        dataProcessHandle.updateEquipmentMonitorPoint(point, UpdateOperationEnum.DELETE);
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
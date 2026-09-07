package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.dto.EquipmentOfflineQueryDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentOffline;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.EquipmentOfflineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备上下线记录接口
 *
 * @author 智能建造团队
 */
@Tag(name = "设备上下线记录", description = "查询设备上下线记录")
@RestController
@RequestMapping("/equipmentOffline")
public class EquipmentOfflineController {

    private final EquipmentOfflineService equipmentOfflineService;

    public EquipmentOfflineController(EquipmentOfflineService equipmentOfflineService) {
        this.equipmentOfflineService = equipmentOfflineService;
    }

    @Operation(summary = "查询设备上下线记录")
    @PostMapping("/selectPage")
    public Result<BPage<EquipmentOffline>> selectPage(@RequestBody(required = false) EquipmentOfflineQueryDTO query) {
        return Result.ok(equipmentOfflineService.selectPage(query));
    }
}
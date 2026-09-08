package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorPointAlertParam;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.MonitorPointAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 设备监测点报警接口
 *
 * <p>提供报警记录的查询与处理操作。</p>
 *
 * @author 智能建造团队
 */
@Tag(name = "设备监测点报警", description = "设备监测点报警的查询、处理等操作接口")
@RestController
@RequestMapping("/monitorPointAlert")
public class EquipmentMonitorPointAlertController {

    private final MonitorPointAlertService monitorPointAlertService;

    public EquipmentMonitorPointAlertController(MonitorPointAlertService monitorPointAlertService) {
        this.monitorPointAlertService = monitorPointAlertService;
    }

    @Operation(summary = "查询报警记录列表",
            description = "根据报警分类、设备报警类型、设备ID、设备名、处理状态、起止时间等条件分页查询报警记录")
    @PostMapping("/selectPage")
    public Result<BPage<MonitorPointAlert>> selectPage(@RequestBody MonitorPointAlertParam monitorPointAlert) {
        return Result.ok(monitorPointAlertService.selectPage(monitorPointAlert));
    }

    @Operation(summary = "处理报警信息", description = "更新报警记录的处理人、处理措施、处理结论，并将状态置为已处理")
    @PostMapping("/update")
    public Result<?> update(@RequestBody MonitorPointAlert monitorPointAlert) {
        monitorPointAlert.setHandleState("1");
        monitorPointAlert.setHandleTime(LocalDateTime.now());
        boolean b = monitorPointAlertService.updateById(monitorPointAlert);
        return b ? Result.ok() : Result.error(-1, "更新失败");
    }
}

package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 监测点警报查询对象
 *
 * <p>包含分页参数和查询条件。</p>
 *
 * @author 智能建造团队
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "监测点警报查询对象，包含分页参数和查询条件")
public class MonitorPointAlertParam extends PageEntity {

    @Schema(description = "报警分类 0预警提醒 1警报提醒 2控制")
    private String alertType;

    @Schema(description = "设备报警类型 1：基坑监测 2：塔机监测 3：升降机监测 9：环境监测")
    private String monitorAlertType;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;

    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;

    @Schema(description = "设备名")
    private String assetName;

    @Schema(description = "处理状态 0：未处理 1：已处理")
    private String handleState;
}

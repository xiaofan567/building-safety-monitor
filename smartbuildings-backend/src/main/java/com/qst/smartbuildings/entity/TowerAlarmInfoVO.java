package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 塔吊报警信息聚合实体
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "塔吊报警信息聚合")
public class TowerAlarmInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总预警数量")
    private Long totalWarning;

    @Schema(description = "总报警数量")
    private Long totalAlarm;

    @Schema(description = "今日预警数量")
    private Long todayWarning;

    @Schema(description = "今日报警数量")
    private Long todayAlarm;

    @Schema(description = "报警类型聚合")
    private List<AlarmTypeItem> typeList;

    @Schema(description = "近30天预警聚合")
    private List<AlarmDayItem> days30Warning;

    @Schema(description = "近30天报警聚合")
    private List<AlarmDayItem> days30Alarm;
}
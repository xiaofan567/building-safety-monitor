package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 高支模/深基坑报警信息聚合实体
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "高支模/深基坑报警信息聚合")
public class HfDfpAlarmInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报警类型聚合")
    private List<HfDfpAlarmTypeItem> alarmTypeList;

    @Schema(description = "近30天报警数量聚合")
    private List<AlarmDayItem> alarmCountList;

    @Schema(description = "近30天预警数量聚合")
    private List<AlarmDayItem> warnCountList;
}

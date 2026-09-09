package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 按日报警聚合项
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "按日报警聚合项")
public class AlarmDayItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报警数量")
    private Long alarmCount;

    @Schema(description = "报警日期")
    private String alarmDate;
}
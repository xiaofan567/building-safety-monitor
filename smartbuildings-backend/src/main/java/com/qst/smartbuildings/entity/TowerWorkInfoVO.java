package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 塔吊今日工作状态聚合实体
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "塔吊今日工作状态聚合")
public class TowerWorkInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总工作时长（秒）")
    private Long totalWorkTime;

    @Schema(description = "总工作次数")
    private Long totalTimes;

    @Schema(description = "今日报警次数")
    private Long totalAlarm;

    @Schema(description = "总吊重")
    private Double totalLoad;

    @Schema(description = "总功效（最大力矩百分比之和）")
    private Double totalEffect;
}
package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 升降机（施工升降电梯）实时数据实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("elevator_data")
@Schema(description = "升降机实时数据实体")
public class ElevatorData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "监测时间")
    private LocalDateTime monitorTime;

    @Schema(description = "前门锁状态 0 关闭 1 开启")
    private String beforeLock;

    @Schema(description = "后门锁状态 0 关闭 1 开启")
    private String afterLock;

    @Schema(description = "X倾角（°）")
    private Double xAngle;

    @Schema(description = "Y倾角（°）")
    private Double yAngle;

    @Schema(description = "行程高度（m）")
    private Integer height;

    @Schema(description = "速度（m/s）")
    private Double speed;

    @Schema(description = "吊重（t）")
    private Double weight;

    @Schema(description = "本次载重百分比")
    private Double weightPercent;

    @Schema(description = "起点高度")
    private Double startHeight;

    @Schema(description = "终点高度")
    private Double endHeight;

    @Schema(description = "平均速度")
    private Double avgSpeed;

    @Schema(description = "起升方向（1静止，2上，3下）")
    private String liftingDirection;

    @Schema(description = "是否违章（0正常 1身份未认证 2重量违章）")
    private String warningState;

    @Schema(description = "人数")
    private Integer numberOfPeople;

    @Schema(description = "风速")
    private Double wind;
}
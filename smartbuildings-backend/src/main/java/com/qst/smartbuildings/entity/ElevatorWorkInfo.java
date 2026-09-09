package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 升降机（施工升降电梯）工作循环记录实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("elevator_work")
@Schema(description = "升降机工作循环记录实体")
public class ElevatorWorkInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "载重")
    private Double weight;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "人数")
    private Integer numberOfPeople;

    @Schema(description = "开始楼层")
    private Integer startFloor;

    @Schema(description = "呼叫楼层")
    private Integer endFloor;

    @Schema(description = "风速")
    private Double wind;

    @Schema(description = "升降机运行速度")
    private Double speed;

    @Schema(description = "倾斜角度")
    private Double angle;

    @Schema(description = "Y倾斜角度")
    private Double angleY;

    @Schema(description = "本次起升起点高度")
    private Double startHeight;

    @Schema(description = "本次起升终点高度")
    private Double endHeight;

    @Schema(description = "本次起升的行程高度")
    private Double height;

    @Schema(description = "本次起升方向")
    private String direction;
}
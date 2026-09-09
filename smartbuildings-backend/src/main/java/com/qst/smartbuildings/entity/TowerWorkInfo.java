package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 塔机工作循环记录实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("tower_work")
@Schema(description = "塔机工作循环记录实体")
public class TowerWorkInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "塔机ID")
    private Long assetId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "平均吊重 t")
    private Double weight;

    @Schema(description = "备注信息")
    private String remarks;

    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最大幅度 m")
    private Double maxRadius;

    @Schema(description = "起始回转角度 °")
    private Double startAngle;

    @Schema(description = "最大力矩百分比 %")
    private Double maxPercent;

    @Schema(description = "最大高度 m")
    private Double maxHeight;

    @Schema(description = "卸吊点的回转角度 °")
    private Double endAngle;

    @Schema(description = "本次吊物过程中的最小幅度 m")
    private Double minRadius;

    @Schema(description = "本次吊物过程中的最大起重量 t")
    private Double maxLoad;

    @Schema(description = "本次吊物最大风速 m/s")
    private Double maxWindSpeed;

    @Schema(description = "本次吊物过程中的最小高度 m")
    private Double minHeight;

    @Schema(description = "起吊点幅度 m")
    private Float liftingPointRadius;

    @Schema(description = "起吊点高度 m")
    private Float liftingPointHeight;

    @Schema(description = "卸吊点幅度 m")
    private Float liftingPointRadiusUnload;

    @Schema(description = "卸吊点高度 m")
    private Float liftingPointHeightUnload;
}
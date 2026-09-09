package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 塔机信息实体
 *
 * <p>存储危大工程塔吊（塔机）设备的基础参数信息。</p>
 *
 * @author 智能建造团队
 */
@Data
@TableName("tower_info")
@Schema(description = "塔机信息实体")
public class TowerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "前向臂长")
    private BigDecimal forwardArmLength;

    @Schema(description = "后向臂长")
    private BigDecimal backwardArmLength;

    @Schema(description = "最大吊高")
    private BigDecimal maxHeight;

    @Schema(description = "额定吊重")
    private BigDecimal ratedLoad;

    @Schema(description = "最大吊重")
    private BigDecimal maxLoad;

    @Schema(description = "额定力矩")
    private BigDecimal ratedMoment;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String assetName;

    @TableField(exist = false)
    @Schema(description = "设备位置名称（联表冗余，非表字段）")
    private String locationName;

    @TableField(exist = false)
    @Schema(description = "平台设备标识（联表冗余，非表字段）")
    private String platformEqCode;
}
package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 升降机（施工升降电梯）信息实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("elevator_info")
@Schema(description = "升降机信息实体")
public class ElevatorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "额定重量")
    private Double ratedWeight;

    @Schema(description = "基本高度")
    private Double basicHeight;

    @Schema(description = "起升速度")
    private Double ascendingSpeed;

    @Schema(description = "额定载重人数")
    private Integer ratedLoad;

    @Schema(description = "吊笼尺寸")
    private Double cabinSize;

    @Schema(description = "最大起升高度")
    private Double maxAscendingHeight;

    @TableField(exist = false)
    @Schema(description = "平台设备标识（联表冗余，非表字段）")
    private String platformEqCode;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String assetName;

    @TableField(exist = false)
    @Schema(description = "设备编号（联表冗余，非表字段）")
    private String assetCode;

    @TableField(exist = false)
    @Schema(description = "设备位置名称（联表冗余，非表字段）")
    private String locationName;
}
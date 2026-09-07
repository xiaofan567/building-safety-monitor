package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备上下线记录实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("equipment_offline")
@Schema(description = "设备上下线记录实体")
public class EquipmentOffline implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "状态 1：上线 2：离线")
    private Integer status;

    @Schema(description = "检测时间")
    private LocalDateTime monitorTime;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String equipmentName;

    @TableField(exist = false)
    @Schema(description = "设备编号（联表冗余，非表字段）")
    private String assetCode;

    @TableField(exist = false)
    @Schema(description = "设备类型ID（联表冗余，非表字段）")
    private String typeId;

    @TableField(exist = false)
    @Schema(description = "设备类型名称（联表冗余，非表字段）")
    private String typeName;
}
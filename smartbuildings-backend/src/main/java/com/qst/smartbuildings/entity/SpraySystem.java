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
 * 自动喷淋系统记录实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("spray_system")
@Schema(description = "自动喷淋系统记录实体")
public class SpraySystem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "监测点ID")
    private Integer monitorPointId;

    @Schema(description = "位置ID")
    private Integer locationId;

    @Schema(description = "设备ID")
    private Integer equipmentId;

    @Schema(description = "触发类型 1：设备监测点越界触发 2：定时任务")
    private Integer triggerType;

    @Schema(description = "操作类型 1：启动 0：关闭 2：操作异常")
    private String operationType;

    @Schema(description = "操作原因")
    private String operationReason;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @TableField(exist = false)
    @Schema(description = "监测点名称（联表冗余，非表字段）")
    private String pointName;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String assetName;

    @TableField(exist = false)
    @Schema(description = "设备位置名称（联表冗余，非表字段）")
    private String assetLocation;
}
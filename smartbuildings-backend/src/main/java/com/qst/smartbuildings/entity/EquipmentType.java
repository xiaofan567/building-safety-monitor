package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备类型信息实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("equipment_type")
@Schema(description = "设备类型信息实体")
public class EquipmentType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "type_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long typeId;

    @Schema(description = "类型编号")
    private String typeCode;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "父级ID，0 表示根节点")
    private Long typePid;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;
}
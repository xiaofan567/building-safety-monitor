package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备位置信息实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("equipment_location")
@Schema(description = "设备位置信息实体")
public class EquipmentLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "location_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long locationId;

    @Schema(description = "编码")
    private String locationCode;

    @Schema(description = "位置名称")
    private String locationName;

    @Schema(description = "父级ID，0 表示根节点")
    private Long locationPid;

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
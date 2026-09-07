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
 * 监测点信息实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("equipment_monitor_point")
@Schema(description = "监测点信息实体")
public class EquipmentMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "监测点ID")
    private Long id;

    @Schema(description = "监测点编号")
    private String pointCode;

    @Schema(description = "监测点名称")
    private String pointName;

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "监测项类型 字典项：1：基坑监测、2：塔机监测、3：升降机监测、9：环境监测")
    private String monitorType;

    @Schema(description = "监测项子类型 字典项：1：PM2.5、2：PM10、3：噪音")
    private String monitorSonType;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "统计类型")
    private String statisticalType;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "排序")
    private String sort;

    @Schema(description = "报警上限阀值")
    private Integer alarmUpper;

    @Schema(description = "报警下限阀值")
    private Integer alarmLower;

    @Schema(description = "预警上限")
    private Integer warnUpper;

    @Schema(description = "预警下限")
    private Integer warnLower;

    @Schema(description = "是否关联喷洒系统 1：是 0：否")
    private String isSpraySystem;

    @Schema(description = "启动喷淋值")
    private Integer startSprayValue;

    @Schema(description = "关闭喷淋值")
    private Integer closeSprayValue;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;

    @Schema(description = "警报范围时间 单位秒")
    private Integer rangeTime;

    @Schema(description = "状态（0：无效 1：启用）")
    private String status;

    @Schema(description = "监测点位置")
    private String pointLocation;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String equipmentName;

    @TableField(exist = false)
    @Schema(description = "实时监测值（非表字段）")
    private String value;

    @TableField(exist = false)
    @Schema(description = "监测时间（非表字段）")
    private String monitorTime;

    @TableField(exist = false)
    @Schema(description = "数据更新时间（非表字段）")
    private String dataUpdateTime;
}
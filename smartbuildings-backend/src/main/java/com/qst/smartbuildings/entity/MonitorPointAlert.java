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
 * 监测点警报实体
 *
 * <p>记录设备监测过程中触发的预警/报警信息，包含报警全生命周期的跟踪数据。</p>
 *
 * @author 智能建造团队
 */
@Data
@TableName("monitor_point_alert")
@Schema(description = "监测点警报实体")
public class MonitorPointAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "报警记录ID")
    private Long id;

    @Schema(description = "监测点ID")
    private Long monitorPointId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "监测数据（报警值）")
    private String monitorData;

    @Schema(description = "报警内容")
    private String content;

    @Schema(description = "监测日志（合并过程中的数据变化）")
    private String monitorLog;

    @Schema(description = "提醒类型 0预警提醒 1警报提醒 2控制")
    private String alertType;

    @Schema(description = "设备报警类型 1：基坑监测、2：塔机监测、3：升降机监测、9：环境监测")
    private String monitorAlertType;

    @Schema(description = "处理状态 0：未处理 1：已处理")
    private String handleState;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理人")
    private String handleBy;

    @Schema(description = "处理措施")
    private String handleMeasures;

    @Schema(description = "处理后结论")
    private String handleConclu;

    @Schema(description = "处理过程图片批次ID")
    private String batchId;

    @Schema(description = "状态恢复时的监测数值")
    private String recoveryData;

    @Schema(description = "状态恢复时间")
    private LocalDateTime recoveryTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String assetName;

    @TableField(exist = false)
    @Schema(description = "设备编号（联表冗余，非表字段）")
    private String assetCode;

    @TableField(exist = false)
    @Schema(description = "报警设备位置名称（联表冗余，非表字段）")
    private String locationName;

    @TableField(exist = false)
    @Schema(description = "监测点名称（联表冗余，非表字段）")
    private String pointName;

    @TableField(exist = false)
    @Schema(description = "监测点类型（联表冗余，非表字段）")
    private String monitorType;

    @TableField(exist = false)
    @Schema(description = "监测项子项（联表冗余，非表字段）")
    private String monitorSonType;

    @TableField(exist = false)
    @Schema(description = "监控点上阈值（联表冗余，非表字段）")
    private String alarmUpper;

    @TableField(exist = false)
    @Schema(description = "监控点下阈值（联表冗余，非表字段）")
    private String alarmLower;

    @TableField(exist = false)
    @Schema(description = "上限值 根据提醒类型取监测点的预警上限或报警上限（联表冗余，非表字段）")
    private String upperValue;

    @TableField(exist = false)
    @Schema(description = "下限值 根据提醒类型取监测点的预警下限或报警下限（联表冗余，非表字段）")
    private String lowerValue;

    @TableField(exist = false)
    @Schema(description = "报警值拼接单位（联表冗余，非表字段）")
    private String unit;
}

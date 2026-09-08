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
 * 监测点监测数据实体
 *
 * <p>记录设备上报的实时监测数据，为报警判断提供数据基础。</p>
 *
 * @author 智能建造团队
 */
@Data
@TableName("monitor_value")
@Schema(description = "监测点监测数据实体")
public class MonitorValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "监测数据记录ID")
    private Long id;

    @Schema(description = "监测点ID")
    private Long monitorPointId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "监测数据（实时数值）")
    private String monitorData;

    @Schema(description = "数据采集时间")
    private LocalDateTime monitorTime;

    @TableField(exist = false)
    @Schema(description = "监测点名称（联表冗余，非表字段）")
    private String monitorPointName;

    @TableField(exist = false)
    @Schema(description = "数据单位（联表冗余，非表字段）")
    private String unit;

    @TableField(exist = false)
    @Schema(description = "设备名称（联表冗余，非表字段）")
    private String equipmentName;

    @TableField(exist = false)
    @Schema(description = "监测点坐标（联表冗余，非表字段）")
    private String location;
}

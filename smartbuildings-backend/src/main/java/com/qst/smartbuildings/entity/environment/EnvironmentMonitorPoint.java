package com.qst.smartbuildings.entity.environment;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("environment_monitor_point")
@Schema(description = "环境监测-监测点设置")
public class EnvironmentMonitorPoint implements Serializable {

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "监测点编号")
    private String pointCode;

    @Schema(description = "监测点名称")
    private String pointName;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "报警上限阀值")
    private BigDecimal alarmUpper;

    @Schema(description = "报警下限阀值")
    private BigDecimal alarmLower;

    @Schema(description = "预警上限")
    private BigDecimal warnUpper;

    @Schema(description = "预警下限")
    private BigDecimal warnLower;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;

    @TableField(exist = false)
    @Schema(description = "更新人名称")
    private String updateUserName;

}

package com.qst.smartbuildings.entity.environment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("environment_value")
@Schema(description = "环境监测数据")
public class EnvironmentValue {
    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @Schema(description = "监测点类型")
    private String monitorPointCode;

    @Schema(description = "监测点设备ID")
    private Integer assetId;
    @Schema(description = "设备名称")
    @TableField(exist = false)
    private String assetName;

    @Schema(description = "监测点名称")
    @TableField(exist = false)
    private String pointName;

    @Schema(description = "监测类型单位")
    @TableField(exist = false)
    private String unit;

    @Schema(description = "监测值")
    private Double value;

    @Schema(description = "监测时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date monitorTime;

    @Schema(description = "日期类型")
    private String dateType;

    @Schema(description = "最大值")
    private Double maxValue;

    @Schema(description = "最小值")
    private Double minValue;

    @Schema(description = "平均值")
    private Double avgValue;

    @Schema(description = "报警上限阀值")
    @TableField(exist = false)
    private BigDecimal alarmUpper;

    @Schema(description = "报警下限阀值")
    @TableField(exist = false)
    private BigDecimal alarmLower;
}

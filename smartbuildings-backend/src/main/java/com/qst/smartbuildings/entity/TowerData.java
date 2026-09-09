package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 塔机设备实时数据实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("tower_data")
@Schema(description = "塔机设备实时数据实体")
public class TowerData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "监测时间")
    private LocalDateTime monitorTime;

    @Schema(description = "吊重（t）")
    private BigDecimal weight;

    @Schema(description = "幅度（m）")
    private BigDecimal amplitude;

    @Schema(description = "倾角（°）")
    private BigDecimal obliguity;

    @Schema(description = "风速（m/s）")
    private BigDecimal windSpeed;

    @Schema(description = "力矩（M*T）")
    private BigDecimal moment;

    @Schema(description = "高度（m）")
    private BigDecimal height;

    @Schema(description = "转角（°）")
    private Integer rotation;
}
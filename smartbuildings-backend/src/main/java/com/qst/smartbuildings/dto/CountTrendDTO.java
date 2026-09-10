package com.qst.smartbuildings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报警/预警数量趋势对象
 *
 * <p>用于机械设备总览，统计昨日数量、对比日数量及环比增长率。</p>
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "报警/预警数量趋势对象")
public class CountTrendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("yesterday_count")
    @Schema(description = "昨日数量")
    private Integer yesterdayCount;

    @JsonProperty("growth_rate")
    @Schema(description = "环比增长率")
    private Integer growthRate;

    @JsonProperty("compare_day_count")
    @Schema(description = "对比日数量")
    private Integer compareDayCount;
}
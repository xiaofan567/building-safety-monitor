package com.qst.smartbuildings.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qst.smartbuildings.dto.CountTrendDTO;
import com.qst.smartbuildings.entity.EquipmentAssets;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 机械设备总览视图对象
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "机械设备总览视图对象")
public class EquipmentOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("equipmentAssetsCount")
    @Schema(description = "设备总数")
    private Integer equipmentAssetsCount;

    @JsonProperty("equipmentAssetsOnlineCount")
    @Schema(description = "在线设备数")
    private Integer equipmentAssetsOnlineCount;

    @JsonProperty("equipmentAssetsList")
    @Schema(description = "设备列表")
    private List<EquipmentAssets> equipmentAssetsList;

    @JsonProperty("alarmCount")
    @Schema(description = "昨日报警数量趋势")
    private CountTrendDTO alarmCount;

    @JsonProperty("warnCount")
    @Schema(description = "昨日预警数量趋势")
    private CountTrendDTO warnCount;
}
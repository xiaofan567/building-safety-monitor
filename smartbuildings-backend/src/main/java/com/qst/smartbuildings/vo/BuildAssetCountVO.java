package com.qst.smartbuildings.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 首页看板危大工程设备统计项
 *
 * <p>字段序列化为下划线风格，与前端接口文档保持一致。</p>
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "首页看板危大工程设备统计项")
public class BuildAssetCountVO {

    @JsonProperty("type_name")
    @Schema(description = "设备类型名称")
    private String typeName;

    @JsonProperty("alert_count")
    @Schema(description = "报警数量")
    private Integer alertCount;

    @JsonProperty("type_id")
    @Schema(description = "设备类型ID")
    private Integer typeId;

    @JsonProperty("total_equipment")
    @Schema(description = "设备总数")
    private Integer totalEquipment;

    @JsonProperty("online_equipment")
    @Schema(description = "在线设备数")
    private Integer onlineEquipment;
}
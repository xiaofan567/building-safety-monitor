package com.qst.smartbuildings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备监测历史数据分页查询参数
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "设备监测历史数据分页查询参数")
public class DeviceDataPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long assetId;

    @Schema(description = "页码")
    private Long page;

    @Schema(description = "每页数量")
    private Long limit;
}
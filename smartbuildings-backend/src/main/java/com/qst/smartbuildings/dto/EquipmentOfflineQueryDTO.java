package com.qst.smartbuildings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询设备上下线记录请求 DTO
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "查询设备上下线记录请求 DTO")
public class EquipmentOfflineQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码")
    private Long page;

    @Schema(description = "每页条数")
    private Long limit;

    @Schema(description = "设备ID")
    private Long equipmentId;
}
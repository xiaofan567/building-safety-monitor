package com.qst.smartbuildings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除监测点请求 DTO
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "删除监测点请求 DTO")
public class EquipmentMonitorPointDeleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "监测点ID不能为空")
    @Schema(description = "监测点ID")
    private Long id;
}
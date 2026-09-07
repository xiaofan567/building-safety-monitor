package com.qst.smartbuildings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除设备类型请求 DTO
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "删除设备类型请求 DTO")
public class EquipmentTypeDeleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "类型ID不能为空")
    @Schema(description = "类型ID")
    private Long typeId;
}
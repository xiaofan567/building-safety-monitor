package com.qst.smartbuildings.entity.param;

import com.qst.smartbuildings.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 喷淋任务配置查询
 *
 * @author system
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "喷淋任务配置查询")
public class SprayTaskParam extends PageEntity {

    @Schema(description = "喷淋任务名称")
    private String taskName;

    @Schema(description = "喷淋任务是否启用")
    private String isEnable;
}
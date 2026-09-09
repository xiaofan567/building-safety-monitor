package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 报警类型聚合项
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "报警类型聚合项")
public class AlarmTypeItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "报警内容")
    private String content;
}
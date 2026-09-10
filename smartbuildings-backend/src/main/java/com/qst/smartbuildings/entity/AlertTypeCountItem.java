package com.qst.smartbuildings.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页看板报警类型聚合项
 *
 * @author 智能建造团队
 */
@Data
@Schema(description = "首页看板报警类型聚合项")
public class AlertTypeCountItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报警内容")
    private String content;

    @Schema(description = "报警数量")
    private Integer numb;
}
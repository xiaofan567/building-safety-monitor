package com.qst.smartbuildings.entity.environment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 喷淋定期任务表
 *
 * @author system
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("spray_task")
@Schema(name = "SprayTask", description = "喷淋定期任务表")
public class SprayTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "启动时间")
    private Date startTime;

    @Schema(description = "下次执行时间")
    private Date nextTime;

    @Schema(description = "喷淋时长，单位：分钟")
    private Integer duration;

    @Schema(description = "执行周期")
    private Integer cycle;

    @Schema(description = "位置ID")
    private Integer locationId;

    @Schema(description = "位置名称")
    @TableField(exist = false)
    private String locationName;

    @Schema(description = "执行周期单位（字典项）")
    private String cycleUnit;

    @Schema(description = "启停状态，0：禁用；1：启用")
    private String isEnable;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
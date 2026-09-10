package com.qst.smartbuildings.entity.param;

import com.qst.smartbuildings.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.util.Date;

/**
 * 喷淋记录查询对象，包含分页参数和查询条件
 *
 * @author system
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "喷淋记录查询对象，包含分页参数和查询条件")
public class SpraySystemParam extends PageEntity {

    @Schema(description = "设备名称")
    private String assetName;

    @Schema(description = "位置名称")
    private String locationName;

    @Schema(description = "操作类型：1=启动，0=关闭，2=操作异常", example = "1", allowableValues = {"1", "0", "2"})
    private String operationType;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;
}
package com.qst.smartbuildings.entity.environment.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qst.smartbuildings.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "设备资产查询对象，包含分页参数和查询条件")
public class EnvironmentValueParam extends PageEntity {
    @Schema(description = "数据类型")
    private String dateType;
    @Schema(description = "监测点类型")
    private String monitorPointCode;
    @Schema(description = "监测点名称")
    private String monitorPointName;
    @Schema(description = "开始时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @Schema(description = "结束时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

}

package com.qst.smartbuildings.entity.camera.param;

import com.qst.smartbuildings.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name = "CameraParam", description = "摄像头查询对象")
public class CameraParam extends PageEntity {
    @Schema(description = "设备名称")
    private String assetName;
    @Schema(description = "位置类型")
    private Integer locationTypeId;
    @Schema(description = "设备编号")
    private String assetCode;
}
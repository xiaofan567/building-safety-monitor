package com.qst.smartbuildings.entity.camera.param;

import com.qst.smartbuildings.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name = "AiAlertBO", description = "AI告警查询对象")
public class AiAlertParam extends PageEntity {
    private String assetCode;
    private String alertType;
    private String alertStatus;
    private String startTime;
    private String endTime;
}

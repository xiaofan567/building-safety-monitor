package com.qst.smartbuildings.entity.camera;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_alert")
@Schema(name = "AiAlert", description = "AI图片识别报警实体类")
public class AiAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "警报时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date alertTime;

    @Schema(description = "警报类型")
    private String alertType;

    @Schema(description = "警报位置")
    @TableField(exist = false)
    private String alertPosition;

    @Schema(description = "警报内容")
    private String alertContent;

    @Schema(description = "警报图片")
    private String alertImg;

    @Schema(description = "警报状态")
    private String alertStatus;

    @Schema(description = "处置人")
    private String handleBy;

    @Schema(description = "处置内容")
    private String handleContent;

    @Schema(description = "处置时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;

    @Schema(description = "设备ID")
    private Integer equipmentId;

    @Schema(description = "设备编号")
    @TableField(exist = false)
    private String code;

    @Schema(description = "设备名称")
    @TableField(exist = false)
    private String assetName;
    public void addAlertType(String type){
        if(alertType==null){
            alertType=type;
        }else{
            alertType=alertType+","+type;
        }
    }
    public void addAlertContent(String content){
        if(alertContent==null){
            alertContent=content;
        }else{
            alertContent=alertContent+","+content;
        }
    }
}

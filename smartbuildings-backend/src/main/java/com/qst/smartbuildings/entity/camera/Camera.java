package com.qst.smartbuildings.entity.camera;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("device_camera")
@Schema(name = "Camera", description = "摄像头设备信息")
public class Camera implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "摄像头设备ID", example = "1001")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "设备资产ID", example = "9001")
    private Integer assetId;

    @Schema(description = "设备名称", example = "南门监控摄像头")
    @TableField(exist = false)
    private String assetName;

    @Schema(description = "设备编号")
    @TableField(exist = false)
    private String assetCode;

    @Schema(description = "状态（1：在线，0：离线）", example = "1")
    private String status;

    @Schema(description = "当前设备是否启用（1：启用，0：禁用）。", example = "1")
    private String isEnable;

    @Schema(description = "wvp视频播放地址", example = "http://localhost:8068/hls/test1/index.m3u8")
    private String wvpOpenUrl;

    @Schema(description = "AI监测类型（字典项）", example = "1,2,3,6")
    private String aiMonitorType;

    @Schema(description = "排序序号", example = "10")
    private Integer sort;

    @TableField(exist = false)
    @Schema(description = "创建时间", example = "2023-06-15T10:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "位置ID", example = "1001")
    @TableField(exist = false)
    private Integer locationId;

    @Schema(description = "位置名称", example = "南门")
    @TableField(exist = false)
    private String locationName;
}

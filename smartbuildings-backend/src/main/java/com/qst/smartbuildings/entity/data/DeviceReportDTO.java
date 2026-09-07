package com.qst.smartbuildings.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * 设备上报数据DTO
 * 用于接收TCP传输的JSON数据
 *
 * @author 智能建造团队
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceReportDTO {

    /**
     * 设备ID
     */
    private String id;

    /**
     * 数据类型（可选）
     */
    private String code;

    /**
     * 动态属性（键值对集合）
     */
    private Map<String, String> properties;
}

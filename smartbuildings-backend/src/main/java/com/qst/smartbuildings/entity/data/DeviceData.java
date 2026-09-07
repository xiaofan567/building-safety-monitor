package com.qst.smartbuildings.entity.data;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备数据实体类（MyBatis-Plus版本）
 * 对应数据库表：device_data
 *
 * @author 智能建造团队
 */
@Data
@TableName("device_data")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID（与JSON中的id对应）
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 数据类型（与JSON中的type对应，可选）
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 动态属性（JSON格式存储）
     */
    @TableField("properties")
    private String properties;

    /**
     * 数据上报时间
     */
    @TableField(value = "report_time", fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;

    /**
     * 将properties JSON字符串转换为Map
     */
    public Map<String, String> getPropertiesAsMap(ObjectMapper objectMapper) {
        try {
            if (this.properties == null || this.properties.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(this.properties, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            // 解析失败返回空Map
            return new HashMap<>();
        }
    }
}

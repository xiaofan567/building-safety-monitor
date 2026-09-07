package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smartbuildings.entity.data.DeviceData;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
import com.qst.smartbuildings.mapper.DeviceDataMapper;
import com.qst.smartbuildings.service.DeviceDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 设备数据服务实现
 *
 * @author 智能建造团队
 */
@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements DeviceDataService {

    private final ObjectMapper objectMapper;

    public DeviceDataServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理设备数据上报
     *
     * @param reportDTO 设备数据上报DTO
     */
    @Override
    public void processDeviceData(DeviceReportDTO reportDTO) {
        // 转换为实体类
        DeviceData deviceData = new DeviceData();
        deviceData.setDeviceId(reportDTO.getId());
        deviceData.setDeviceCode(reportDTO.getCode());
        deviceData.setReportTime(LocalDateTime.now());
        try {
            String propertiesJson = objectMapper.writeValueAsString(reportDTO.getProperties());
            deviceData.setProperties(propertiesJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("设备属性序列化失败", e);
        }
        // 保存到数据库
        this.save(deviceData);
    }
}

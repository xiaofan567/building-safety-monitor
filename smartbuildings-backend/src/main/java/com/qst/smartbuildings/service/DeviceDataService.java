package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.data.DeviceData;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;

/**
 * 设备数据服务
 *
 * @author 智能建造团队
 */
public interface DeviceDataService extends IService<DeviceData> {

    /**
     * 处理设备数据上报（解析后存入数据库）
     *
     * @param reportDTO 设备数据上报DTO
     */
    void processDeviceData(DeviceReportDTO reportDTO);
}

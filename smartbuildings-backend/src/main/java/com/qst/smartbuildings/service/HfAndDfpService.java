package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.HfDfpAlarmInfoVO;
import com.qst.smartbuildings.entity.MonitorValue;

import java.util.List;
import java.util.Map;

/**
 * 高支模/深基坑业务服务
 *
 * @author 智能建造团队
 */
public interface HfAndDfpService {

    /**
     * 查询高支模列表
     */
    List<EquipmentAssets> hfList();

    /**
     * 查询深基坑列表
     */
    List<EquipmentAssets> dfpList();

    /**
     * 查询高支模/深基坑监测数据（按监测点名称分组，每个监测点最多10条）
     */
    Map<String, List<MonitorValue>> getMonitorData(Long assetId);

    /**
     * 查询高支模/深基坑报警数据聚合
     */
    HfDfpAlarmInfoVO getAlarmInfo(Long assetId);
}

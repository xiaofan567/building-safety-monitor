package com.qst.smartbuildings.service;

import com.qst.smartbuildings.vo.EquipmentOverviewVO;

/**
 * 机械设备总览业务服务
 *
 * @author 智能建造团队
 */
public interface BuildViewService {

    /**
     * 根据设备类型ID查询机械设备总览
     *
     * @param typeId 设备类型ID（如：145塔吊、115升降机、147高支模、148深基坑）
     * @return 机械设备总览视图对象
     */
    EquipmentOverviewVO overview(Long typeId);
}
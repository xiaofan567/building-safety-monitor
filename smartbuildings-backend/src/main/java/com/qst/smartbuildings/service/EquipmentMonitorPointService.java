package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;

/**
 * 监测点信息服务
 *
 * @author 智能建造团队
 */
public interface EquipmentMonitorPointService {

    /**
     * 分页查询监测点列表（联表补充设备名称）
     *
     * @param current     当前页码
     * @param size        每页条数
     * @param monitorType 监测点类型（可选）
     * @param pointName   监测点名称（可选，模糊匹配）
     * @return 监测点分页数据
     */
    BPage<EquipmentMonitorPoint> pageList(long current, long size, String monitorType, String pointName);

    /**
     * 查询监测点详情
     *
     * @param id 监测点ID
     * @return 监测点详情
     */
    EquipmentMonitorPoint getById(Long id);

    /**
     * 新增或更新监测点信息（id 为空时新增，否则更新）
     *
     * @param equipmentMonitorPoint 监测点信息
     */
    void savePoint(EquipmentMonitorPoint equipmentMonitorPoint);

    /**
     * 删除监测点
     *
     * @param id 监测点ID
     */
    void delete(Long id);
}

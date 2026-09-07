package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.EquipmentLocation;
import com.qst.smartbuildings.vo.EquipmentLocationTreeVO;

import java.util.List;

/**
 * 设备位置信息服务
 *
 * @author 智能建造团队
 */
public interface EquipmentLocationService {

    /**
     * 查询地址树
     *
     * @return 地址树列表
     */
    List<EquipmentLocationTreeVO> queryTreeList();

    /**
     * 查询地址详情
     *
     * @param locationId 位置ID
     * @return 位置详情
     */
    EquipmentLocation getById(Long locationId);

    /**
     * 新增或修改地址（locationId 为空时新增，否则修改）
     *
     * @param equipmentLocation 位置信息
     */
    void saveLocation(EquipmentLocation equipmentLocation);

    /**
     * 删除地址，存在下级地址时无法删除
     *
     * @param locationId 位置ID
     */
    void delete(Long locationId);
}
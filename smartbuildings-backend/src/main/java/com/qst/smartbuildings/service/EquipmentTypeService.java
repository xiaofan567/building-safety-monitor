package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.vo.EquipmentTypeTreeVO;

import java.util.List;

/**
 * 设备类型信息服务
 *
 * @author 智能建造团队
 */
public interface EquipmentTypeService {

    /**
     * 查询类型树
     *
     * @return 类型树列表
     */
    List<EquipmentTypeTreeVO> queryTreeList();

    /**
     * 查询类型详情
     *
     * @param typeId 类型ID
     * @return 类型详情
     */
    EquipmentType getById(Long typeId);

    /**
     * 新增或修改设备类型（typeId 为空时新增，否则修改）
     *
     * @param equipmentType 类型信息
     */
    void saveType(EquipmentType equipmentType);

    /**
     * 删除设备类型，存在下级类型或类型下存在设备时无法删除
     *
     * @param typeId 类型ID
     */
    void delete(Long typeId);
}
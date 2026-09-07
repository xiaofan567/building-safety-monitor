package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentAssets;

/**
 * 设备资产管理服务
 *
 * @author 智能建造团队
 */
public interface EquipmentAssetsService {

    /**
     * 分页查询启用状态的设备列表（联表补充分页位置名称、类型名称）
     *
     * @param current 当前页码
     * @param size    每页条数
     * @return 设备分页数据
     */
    BPage<EquipmentAssets> pageList(long current, long size);

    /**
     * 查询设备详情
     *
     * @param assetId 资产ID
     * @return 设备详情
     */
    EquipmentAssets getById(Long assetId);

    /**
     * 新增或修改设备（assetId 为空时新增，否则修改）
     *
     * @param equipmentAssets 设备信息
     */
    void saveAssets(EquipmentAssets equipmentAssets);

    /**
     * 删除设备
     *
     * @param assetId 资产ID
     */
    void delete(Long assetId);
}

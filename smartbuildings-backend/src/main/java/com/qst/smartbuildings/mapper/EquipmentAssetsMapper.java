package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.EquipmentAssets;

import java.util.List;
import java.util.Map;

/**
 * 设备资产信息 Mapper
 *
 * @author 智能建造团队
 */
public interface EquipmentAssetsMapper extends BaseMapper<EquipmentAssets> {

    /**
     * 按设备类型统计设备总数与在线数，供日报总览使用。
     *
     * @return 每行包含 typeId / typeName / totalEquipment / onlineEquipment
     */
    List<Map<String, Object>> getBuildAssetCount();
}

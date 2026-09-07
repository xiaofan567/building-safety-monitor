package com.qst.smartbuildings.service;

import com.qst.smartbuildings.dto.EquipmentOfflineQueryDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentOffline;

/**
 * 设备上下线记录服务
 *
 * @author 智能建造团队
 */
public interface EquipmentOfflineService {

    /**
     * 分页查询设备上下线记录（联表补充设备信息），按检测时间倒序
     *
     * @param query 查询条件（页码、每页条数、设备ID）
     * @return 上下线记录分页数据
     */
    BPage<EquipmentOffline> selectPage(EquipmentOfflineQueryDTO query);
}
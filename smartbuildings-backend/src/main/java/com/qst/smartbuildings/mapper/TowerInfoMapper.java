package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.AlarmDayItem;
import com.qst.smartbuildings.entity.AlarmTypeItem;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.TowerWorkInfoVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 塔机信息 Mapper
 *
 * @author 智能建造团队
 */
public interface TowerInfoMapper extends BaseMapper<TowerInfo> {

    /**
     * 查询塔吊列表（联表补充设备名称、位置名称、平台设备标识）
     */
    List<TowerInfo> selectAllTower(@Param("typeId") Long typeId);

    /**
     * 查询全部塔机信息列表（联表补充平台设备标识，用于数据处理组件内存初始化）
     */
    List<TowerInfo> getTowerList();

    /**
     * 查询塔吊今日工作状态聚合
     */
    TowerWorkInfoVO selectTowerWorkInfo(@Param("assetId") Long assetId);

    /**
     * 按报警内容分组统计报警数量
     */
    List<AlarmTypeItem> selectAlarmTypeList(@Param("assetId") Long assetId,
                                            @Param("monitorAlertType") String monitorAlertType);

    /**
     * 按日期分组统计近30天预警/报警数量
     */
    List<AlarmDayItem> selectAlarmDays(@Param("assetId") Long assetId,
                                       @Param("monitorAlertType") String monitorAlertType,
                                       @Param("alertType") String alertType);
}
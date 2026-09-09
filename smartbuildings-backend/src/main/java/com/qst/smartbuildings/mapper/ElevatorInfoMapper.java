package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.AlarmDayItem;
import com.qst.smartbuildings.entity.AlarmTypeItem;
import com.qst.smartbuildings.entity.ElevatorInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 升降机信息 Mapper
 *
 * @author 智能建造团队
 */
public interface ElevatorInfoMapper extends BaseMapper<ElevatorInfo> {

    /**
     * 查询升降机列表（联表补充设备名称、设备编号、位置名称）
     */
    List<ElevatorInfo> selectAllElevator(@Param("typeId") Long typeId);

    /**
     * 查询全部升降机（联表补充平台设备标识，用于数据处理组件初始化）
     */
    List<ElevatorInfo> getAllElevator();

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
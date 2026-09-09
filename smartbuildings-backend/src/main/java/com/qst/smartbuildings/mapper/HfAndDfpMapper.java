package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.AlarmDayItem;
import com.qst.smartbuildings.entity.HfDfpAlarmTypeItem;
import com.qst.smartbuildings.entity.MonitorValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 高支模/深基坑 Mapper
 *
 * @author 智能建造团队
 */
public interface HfAndDfpMapper extends BaseMapper<MonitorValue> {

    /**
     * 查询设备监测数据（联表补充监测点名称、单位、设备名称，按监测时间倒序）
     */
    List<MonitorValue> selectMonitorValueList(@Param("assetId") Long assetId);

    /**
     * 按报警内容分组统计报警数量
     */
    List<HfDfpAlarmTypeItem> selectAlarmTypeList(@Param("assetId") Long assetId,
                                                 @Param("monitorAlertType") String monitorAlertType);

    /**
     * 按日期分组统计近30天预警/报警数量
     */
    List<AlarmDayItem> selectAlarmDays(@Param("assetId") Long assetId,
                                       @Param("monitorAlertType") String monitorAlertType,
                                       @Param("alertType") String alertType);
}

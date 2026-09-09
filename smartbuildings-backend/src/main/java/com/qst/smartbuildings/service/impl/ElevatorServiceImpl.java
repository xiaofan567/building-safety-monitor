package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.ElevatorAlarmInfoVO;
import com.qst.smartbuildings.entity.ElevatorData;
import com.qst.smartbuildings.entity.ElevatorInfo;
import com.qst.smartbuildings.entity.ElevatorWorkInfo;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.mapper.ElevatorDataMapper;
import com.qst.smartbuildings.mapper.ElevatorInfoMapper;
import com.qst.smartbuildings.mapper.ElevatorWorkInfoMapper;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.service.ElevatorService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 升降机（施工升降电梯）业务服务实现
 *
 * @author 智能建造团队
 */
@Service
public class ElevatorServiceImpl extends ServiceImpl<ElevatorInfoMapper, ElevatorInfo> implements ElevatorService {

    /** 升降机监测报警类型 */
    private static final String ELEVATOR_ALERT_TYPE = "3";
    /** 预警 */
    private static final String ALERT_TYPE_WARNING = "0";
    /** 报警 */
    private static final String ALERT_TYPE_ALARM = "1";

    private final ElevatorDataMapper elevatorDataMapper;
    private final ElevatorWorkInfoMapper elevatorWorkInfoMapper;
    private final MonitorPointAlertMapper monitorPointAlertMapper;

    public ElevatorServiceImpl(ElevatorDataMapper elevatorDataMapper,
                               ElevatorWorkInfoMapper elevatorWorkInfoMapper,
                               MonitorPointAlertMapper monitorPointAlertMapper) {
        this.elevatorDataMapper = elevatorDataMapper;
        this.elevatorWorkInfoMapper = elevatorWorkInfoMapper;
        this.monitorPointAlertMapper = monitorPointAlertMapper;
    }

    @Override
    public List<ElevatorInfo> selectAllElevator() {
        return baseMapper.selectAllElevator(115L);
    }

    @Override
    public BPage<ElevatorData> elevatorDataList(Long assetId, long page, long limit) {
        validateAssetId(assetId);
        BPage<ElevatorData> pageResult = new BPage<>(page, limit);
        elevatorDataMapper.selectPage(pageResult, new LambdaQueryWrapper<ElevatorData>()
                .eq(ElevatorData::getAssetId, assetId)
                .orderByDesc(ElevatorData::getMonitorTime));
        return pageResult;
    }

    @Override
    public BPage<ElevatorWorkInfo> elevatorWorkList(Long assetId, long page, long limit) {
        validateAssetId(assetId);
        BPage<ElevatorWorkInfo> pageResult = new BPage<>(page, limit);
        elevatorWorkInfoMapper.selectPage(pageResult, new LambdaQueryWrapper<ElevatorWorkInfo>()
                .eq(ElevatorWorkInfo::getAssetId, assetId)
                .orderByDesc(ElevatorWorkInfo::getStartTime));
        return pageResult;
    }

    @Override
    public List<ElevatorData> getElevatorData(Long assetId) {
        validateAssetId(assetId);
        return elevatorDataMapper.selectList(new LambdaQueryWrapper<ElevatorData>()
                .eq(ElevatorData::getAssetId, assetId)
                .orderByDesc(ElevatorData::getMonitorTime)
                .last("LIMIT 10"));
    }

    @Override
    public ElevatorAlarmInfoVO getElevatorAlarmInfo(Long assetId) {
        validateAssetId(assetId);
        ElevatorAlarmInfoVO vo = new ElevatorAlarmInfoVO();
        vo.setTotalWarning(countAlarm(assetId, ALERT_TYPE_WARNING));
        vo.setTotalAlarm(countAlarm(assetId, ALERT_TYPE_ALARM));
        vo.setTodayWarning(countTodayAlarm(assetId, ALERT_TYPE_WARNING));
        vo.setTodayAlarm(countTodayAlarm(assetId, ALERT_TYPE_ALARM));
        vo.setTypeList(baseMapper.selectAlarmTypeList(assetId, ELEVATOR_ALERT_TYPE));
        vo.setDays30Warning(baseMapper.selectAlarmDays(assetId, ELEVATOR_ALERT_TYPE, ALERT_TYPE_WARNING));
        vo.setDays30Alarm(baseMapper.selectAlarmDays(assetId, ELEVATOR_ALERT_TYPE, ALERT_TYPE_ALARM));
        return vo;
    }

    /**
     * 统计设备某类报警（预警/报警）的总数
     */
    private Long countAlarm(Long assetId, String alertType) {
        return monitorPointAlertMapper.selectCount(new LambdaQueryWrapper<MonitorPointAlert>()
                .eq(MonitorPointAlert::getEquipmentId, assetId)
                .eq(MonitorPointAlert::getMonitorAlertType, ELEVATOR_ALERT_TYPE)
                .eq(MonitorPointAlert::getAlertType, alertType));
    }

    /**
     * 统计设备今日某类报警（预警/报警）的数量
     */
    private Long countTodayAlarm(Long assetId, String alertType) {
        LocalDate today = LocalDate.now();
        return monitorPointAlertMapper.selectCount(new LambdaQueryWrapper<MonitorPointAlert>()
                .eq(MonitorPointAlert::getEquipmentId, assetId)
                .eq(MonitorPointAlert::getMonitorAlertType, ELEVATOR_ALERT_TYPE)
                .eq(MonitorPointAlert::getAlertType, alertType)
                .between(MonitorPointAlert::getCreateTime, today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
    }

    /**
     * 校验设备ID
     */
    private void validateAssetId(Long assetId) {
        if (assetId == null || Objects.equals(assetId, 0L)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
    }
}
package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.TowerAlarmInfoVO;
import com.qst.smartbuildings.entity.TowerData;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.TowerWorkInfo;
import com.qst.smartbuildings.entity.TowerWorkInfoVO;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.mapper.TowerDataMapper;
import com.qst.smartbuildings.mapper.TowerInfoMapper;
import com.qst.smartbuildings.mapper.TowerWorkInfoMapper;
import com.qst.smartbuildings.service.TowerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 塔吊（塔机）业务服务实现
 *
 * @author 智能建造团队
 */
@Service
public class TowerServiceImpl extends ServiceImpl<TowerInfoMapper, TowerInfo> implements TowerService {

    /** 塔机监测报警类型 */
    private static final String TOWER_ALERT_TYPE = "2";
    /** 预警 */
    private static final String ALERT_TYPE_WARNING = "0";
    /** 报警 */
    private static final String ALERT_TYPE_ALARM = "1";

    private final TowerDataMapper towerDataMapper;
    private final TowerWorkInfoMapper towerWorkInfoMapper;
    private final MonitorPointAlertMapper monitorPointAlertMapper;

    public TowerServiceImpl(TowerDataMapper towerDataMapper,
                            TowerWorkInfoMapper towerWorkInfoMapper,
                            MonitorPointAlertMapper monitorPointAlertMapper) {
        this.towerDataMapper = towerDataMapper;
        this.towerWorkInfoMapper = towerWorkInfoMapper;
        this.monitorPointAlertMapper = monitorPointAlertMapper;
    }

    @Override
    public List<TowerInfo> selectAllTower() {
        return baseMapper.selectAllTower(145L);
    }

    @Override
    public BPage<TowerData> towerDataList(Long assetId, long page, long limit) {
        validateAssetId(assetId);
        BPage<TowerData> pageResult = new BPage<>(page, limit);
        towerDataMapper.selectPage(pageResult, new LambdaQueryWrapper<TowerData>()
                .eq(TowerData::getAssetId, assetId)
                .orderByDesc(TowerData::getMonitorTime));
        return pageResult;
    }

    @Override
    public BPage<TowerWorkInfo> towerWorkList(Long assetId, long page, long limit) {
        validateAssetId(assetId);
        BPage<TowerWorkInfo> pageResult = new BPage<>(page, limit);
        towerWorkInfoMapper.selectPage(pageResult, new LambdaQueryWrapper<TowerWorkInfo>()
                .eq(TowerWorkInfo::getAssetId, assetId)
                .orderByDesc(TowerWorkInfo::getStartTime));
        return pageResult;
    }

    @Override
    public List<TowerData> getTowerData(Long assetId) {
        validateAssetId(assetId);
        return towerDataMapper.selectList(new LambdaQueryWrapper<TowerData>()
                .eq(TowerData::getAssetId, assetId)
                .orderByDesc(TowerData::getMonitorTime)
                .last("LIMIT 10"));
    }

    @Override
    public TowerWorkInfoVO getTowerWorkInfo(Long assetId) {
        validateAssetId(assetId);
        TowerWorkInfoVO vo = baseMapper.selectTowerWorkInfo(assetId);
        if (vo == null) {
            vo = new TowerWorkInfoVO();
        }
        vo.setTotalAlarm(countTodayAlarm(assetId, ALERT_TYPE_ALARM));
        return vo;
    }

    @Override
    public TowerAlarmInfoVO getTowerAlarmInfo(Long assetId) {
        validateAssetId(assetId);
        TowerAlarmInfoVO vo = new TowerAlarmInfoVO();
        vo.setTotalWarning(countAlarm(assetId, ALERT_TYPE_WARNING));
        vo.setTotalAlarm(countAlarm(assetId, ALERT_TYPE_ALARM));
        vo.setTodayWarning(countTodayAlarm(assetId, ALERT_TYPE_WARNING));
        vo.setTodayAlarm(countTodayAlarm(assetId, ALERT_TYPE_ALARM));
        vo.setTypeList(baseMapper.selectAlarmTypeList(assetId, TOWER_ALERT_TYPE));
        vo.setDays30Warning(baseMapper.selectAlarmDays(assetId, TOWER_ALERT_TYPE, ALERT_TYPE_WARNING));
        vo.setDays30Alarm(baseMapper.selectAlarmDays(assetId, TOWER_ALERT_TYPE, ALERT_TYPE_ALARM));
        return vo;
    }

    /**
     * 统计设备某类报警（预警/报警）的总数
     */
    private Long countAlarm(Long assetId, String alertType) {
        return monitorPointAlertMapper.selectCount(new LambdaQueryWrapper<MonitorPointAlert>()
                .eq(MonitorPointAlert::getEquipmentId, assetId)
                .eq(MonitorPointAlert::getMonitorAlertType, TOWER_ALERT_TYPE)
                .eq(MonitorPointAlert::getAlertType, alertType));
    }

    /**
     * 统计设备今日某类报警（预警/报警）的数量
     */
    private Long countTodayAlarm(Long assetId, String alertType) {
        LocalDate today = LocalDate.now();
        return monitorPointAlertMapper.selectCount(new LambdaQueryWrapper<MonitorPointAlert>()
                .eq(MonitorPointAlert::getEquipmentId, assetId)
                .eq(MonitorPointAlert::getMonitorAlertType, TOWER_ALERT_TYPE)
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
package com.qst.smartbuildings.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smartbuildings.common.EquipmentTypeConst;
import com.qst.smartbuildings.entity.ElevatorData;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentOffline;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorValue;
import com.qst.smartbuildings.entity.TowerData;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.mapper.AiAlertMapper;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentOfflineMapper;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.service.ElevatorService;
import com.qst.smartbuildings.service.EnvironmentValueService;
import com.qst.smartbuildings.service.MonitorPointValueService;
import com.qst.smartbuildings.service.SecurityDailyReportService;
import com.qst.smartbuildings.service.TowerService;
import com.qst.smartbuildings.vo.SecurityDailyReportVo;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.AiAlertsSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.ElevatorDevice;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.ElevatorSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.EnvironmentSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.FormworkDevice;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.FormworkSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.FoundationPitDevice;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.FoundationPitSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.MonitorAlertsSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.OfflineRecordsSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.Overview;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.TowerCraneSection;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.TowerDevice;
import com.qst.smartbuildings.vo.SecurityDailyReportVo.TypeStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 安全日报数据组装服务实现。
 *
 * <p>查询设备总览、塔吊、升降机、高支模、深基坑、环境、AI 报警、
 * 设备报警与离线记录等数据，拼装成完整的 SecurityDailyReportVo。</p>
 */
@Service
public class SecurityDailyReportServiceImpl implements SecurityDailyReportService {

    private static final Logger log = LoggerFactory.getLogger(SecurityDailyReportServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private EquipmentAssetsMapper equipmentAssetsMapper;
    @Autowired
    private TowerService towerService;
    @Autowired
    private ElevatorService elevatorService;
    @Autowired
    private MonitorPointValueService monitorPointValueService;
    @Autowired
    private MonitorPointAlertMapper monitorPointAlertMapper;
    @Autowired
    private AiAlertMapper aiAlertMapper;
    @Autowired
    private EnvironmentValueService environmentValueService;
    @Autowired
    private EquipmentOfflineMapper equipmentOfflineMapper;

    @Override
    public SecurityDailyReportVo getDailyReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart.minusSeconds(1);
        LocalDateTime reportEnd = todayStart.plusDays(1).withHour(8).minusSeconds(1);

        Date nowDate = DateUtil.date();
        Date todayStartDate = DateUtil.date(todayStart);
        Date yesterdayStartDate = DateUtil.date(yesterdayStart);
        Date yesterdayEndDate = DateUtil.date(yesterdayEnd);

        SecurityDailyReportVo report = new SecurityDailyReportVo()
                .setReportDate(now.format(DATE_TIME_FMT))
                .setReportPeriod(todayStart.withHour(8).format(DATE_TIME_FMT) + " ~ " + reportEnd.format(DATE_TIME_FMT))
                .setStartDate(todayStart.format(DATE_TIME_FMT))
                .setEndDate(reportEnd.format(DATE_TIME_FMT))
                .setOverview(buildOverview(todayStartDate, yesterdayStartDate, yesterdayEndDate))
                .setTowerCrane(buildTowerCraneSection())
                .setElevator(buildElevatorSection())
                .setFormwork(buildFormworkSection())
                .setFoundationPit(buildFoundationPitSection())
                .setEnvironment(buildEnvironmentSection())
                .setAiAlerts(buildAiAlertsSection(todayStartDate))
                .setMonitorAlerts(buildMonitorAlertsSection(todayStartDate))
                .setOfflineRecords(buildOfflineRecordsSection(todayStartDate));
        return report;
    }

    private Overview buildOverview(Date todayStartDate, Date yesterdayStartDate, Date yesterdayEndDate) {
        List<Map<String, Object>> buildAssetCount;
        try {
            buildAssetCount = equipmentAssetsMapper.getBuildAssetCount();
        } catch (Exception e) {
            log.warn("查询设备总览失败，使用空数据: {}", e.getMessage());
            buildAssetCount = Collections.emptyList();
        }

        List<TypeStat> typeStats = new ArrayList<>();
        int totalAll = 0;
        int onlineAll = 0;

        for (Map<String, Object> row : buildAssetCount) {
            TypeStat stat = new TypeStat();
            stat.setTypeName(row.get("typeName") != null ? row.get("typeName").toString() : "");
            stat.setTypeId(toInt(row.get("typeId")));
            stat.setTotal(toInt(row.get("totalEquipment")));
            stat.setOnline(toInt(row.get("onlineEquipment")));

            List<Long> assetIds = getAssetIdsByType(stat.getTypeId());
            stat.setAlarmToday(countAlerts(assetIds, todayStartDate, nowDate(), "1"));
            stat.setAlarmYesterday(countAlerts(assetIds, yesterdayStartDate, yesterdayEndDate, "1"));
            stat.setWarnToday(countAlerts(assetIds, todayStartDate, nowDate(), "0"));
            stat.setWarnYesterday(countAlerts(assetIds, yesterdayStartDate, yesterdayEndDate, "0"));

            typeStats.add(stat);
            totalAll += stat.getTotal() == null ? 0 : stat.getTotal();
            onlineAll += stat.getOnline() == null ? 0 : stat.getOnline();
        }

        Overview overview = new Overview();
        overview.setTypeStats(typeStats);
        overview.setOverallOnlineRate(totalAll > 0 ? Math.round(onlineAll * 1000.0 / totalAll) / 10.0 : 0.0);
        return overview;
    }

    private List<Long> getAssetIdsByType(Integer typeId) {
        if (typeId == null) {
            return Collections.emptyList();
        }
        List<EquipmentAssets> list = equipmentAssetsMapper.selectList(
                new LambdaQueryWrapper<EquipmentAssets>()
                        .eq(EquipmentAssets::getTypeId, typeId.longValue())
                        .eq(EquipmentAssets::getStatus, "1")
        );
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(EquipmentAssets::getAssetId).collect(Collectors.toList());
    }

    private int countAlerts(List<Long> assetIds, Date start, Date end, String alertType) {
        if (assetIds.isEmpty()) return 0;
        try {
            return monitorPointAlertMapper.selectCount(
                    new LambdaQueryWrapper<MonitorPointAlert>()
                            .in(MonitorPointAlert::getEquipmentId, assetIds)
                            .eq(MonitorPointAlert::getAlertType, alertType)
                            .ge(MonitorPointAlert::getCreateTime, start)
                            .le(MonitorPointAlert::getCreateTime, end)
            ).intValue();
        } catch (Exception e) {
            log.warn("统计报警数量失败: {}", e.getMessage());
            return 0;
        }
    }

    private TowerCraneSection buildTowerCraneSection() {
        List<EquipmentAssets> towerAssets;
        try {
            towerAssets = equipmentAssetsMapper.selectList(
                    new LambdaQueryWrapper<EquipmentAssets>()
                            .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.TOWER_TYPE_ID.longValue())
                            .eq(EquipmentAssets::getStatus, "1")
            );
            if (towerAssets == null) towerAssets = Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询塔吊设备列表失败: {}", e.getMessage());
            towerAssets = Collections.emptyList();
        }

        List<TowerDevice> devices = new ArrayList<>();
        for (EquipmentAssets asset : towerAssets) {
            TowerDevice device = new TowerDevice();
            device.setAssetId(asset.getAssetId());
            device.setDeviceName(asset.getAssetName());
            try {
                List<TowerData> dataList = towerService.getTowerData(asset.getAssetId());
                if (dataList != null && !dataList.isEmpty()) {
                    TowerData latest = dataList.get(0);
                    device.setWeight(toFloat(latest.getWeight()));
                    device.setMoment(toFloat(latest.getMoment()));
                    device.setAmplitude(toFloat(latest.getAmplitude()));
                    device.setRotation(latest.getRotation() == null ? null : latest.getRotation().floatValue());
                    device.setWindSpeed(toFloat(latest.getWindSpeed()));
                    device.setHeight(toFloat(latest.getHeight()));
                }
            } catch (Exception e) {
                log.warn("查询塔吊实时数据失败 assetId={}: {}", asset.getAssetId(), e.getMessage());
            }
            devices.add(device);
        }

        TowerCraneSection section = new TowerCraneSection();
        section.setDevices(devices);
        section.setAlarms(new ArrayList<>());
        return section;
    }

    private ElevatorSection buildElevatorSection() {
        List<EquipmentAssets> elevatorAssets;
        try {
            elevatorAssets = equipmentAssetsMapper.selectList(
                    new LambdaQueryWrapper<EquipmentAssets>()
                            .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.ELEVATOR_TYPE_ID.longValue())
                            .eq(EquipmentAssets::getStatus, "1")
            );
            if (elevatorAssets == null) elevatorAssets = Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询升降机设备列表失败: {}", e.getMessage());
            elevatorAssets = Collections.emptyList();
        }

        List<ElevatorDevice> devices = new ArrayList<>();
        for (EquipmentAssets asset : elevatorAssets) {
            ElevatorDevice device = new ElevatorDevice();
            device.setAssetId(asset.getAssetId());
            device.setDeviceName(asset.getAssetName());
            try {
                List<ElevatorData> dataList = elevatorService.getElevatorData(asset.getAssetId());
                if (dataList != null && !dataList.isEmpty()) {
                    ElevatorData latest = dataList.get(0);
                    device.setWeight(latest.getWeight());
                    device.setWeightPercent(latest.getWeightPercent());
                    device.setHeight(latest.getHeight() == null ? null : latest.getHeight().doubleValue());
                    device.setXAngle(latest.getXAngle());
                    device.setYAngle(latest.getYAngle());
                    device.setWind(latest.getWind());
                    device.setSpeed(latest.getSpeed());
                }
            } catch (Exception e) {
                log.warn("查询升降机实时数据失败 assetId={}: {}", asset.getAssetId(), e.getMessage());
            }
            devices.add(device);
        }

        ElevatorSection section = new ElevatorSection();
        section.setDevices(devices);
        section.setAlarms(new ArrayList<>());
        return section;
    }

    private FormworkSection buildFormworkSection() {
        List<EquipmentAssets> formworkAssets;
        try {
            formworkAssets = equipmentAssetsMapper.selectList(
                    new LambdaQueryWrapper<EquipmentAssets>()
                            .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.HIGH_FORMWORK_TYPE_ID.longValue())
                            .eq(EquipmentAssets::getStatus, "1")
            );
            if (formworkAssets == null) formworkAssets = Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询高支模设备列表失败: {}", e.getMessage());
            formworkAssets = Collections.emptyList();
        }

        List<FormworkDevice> devices = new ArrayList<>();
        for (EquipmentAssets asset : formworkAssets) {
            FormworkDevice device = new FormworkDevice();
            device.setAssetId(asset.getAssetId());
            device.setDeviceName(asset.getAssetName());
            device.setMonitorPoints(buildMonitorPointMap(asset.getAssetId()));
            devices.add(device);
        }

        FormworkSection section = new FormworkSection();
        section.setDevices(devices);
        return section;
    }

    private FoundationPitSection buildFoundationPitSection() {
        List<EquipmentAssets> pitAssets;
        try {
            pitAssets = equipmentAssetsMapper.selectList(
                    new LambdaQueryWrapper<EquipmentAssets>()
                            .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.Deep_Foundation_PIT_TYPE_ID.longValue())
                            .eq(EquipmentAssets::getStatus, "1")
            );
            if (pitAssets == null) pitAssets = Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询深基坑设备列表失败: {}", e.getMessage());
            pitAssets = Collections.emptyList();
        }

        List<FoundationPitDevice> devices = new ArrayList<>();
        for (EquipmentAssets asset : pitAssets) {
            FoundationPitDevice device = new FoundationPitDevice();
            device.setAssetId(asset.getAssetId());
            device.setDeviceName(asset.getAssetName());
            device.setMonitorPoints(buildMonitorPointMap(asset.getAssetId()));
            devices.add(device);
        }

        FoundationPitSection section = new FoundationPitSection();
        section.setDevices(devices);
        section.setAlarms(new ArrayList<>());
        return section;
    }

    private Map<String, Object> buildMonitorPointMap(Long assetId) {
        try {
            List<MonitorValue> values = monitorPointValueService.list(
                    new LambdaQueryWrapper<MonitorValue>()
                            .eq(MonitorValue::getEquipmentId, assetId)
                            .orderByDesc(MonitorValue::getMonitorTime)
                            .last("limit 20")
            );
            Map<String, Object> points = new HashMap<>();
            if (values != null) {
                for (MonitorValue v : values) {
                    String key = v.getMonitorPointName() != null ? v.getMonitorPointName() : String.valueOf(v.getMonitorPointId());
                    points.putIfAbsent(key, v.getMonitorData());
                }
            }
            return points;
        } catch (Exception e) {
            log.warn("查询监测点数据失败 assetId={}: {}", assetId, e.getMessage());
            return new HashMap<>();
        }
    }

    private EnvironmentSection buildEnvironmentSection() {
        Map<String, Object> envInfo;
        try {
            envInfo = environmentValueService.getEnvironmentInfo();
            if (envInfo == null) {
                envInfo = new HashMap<>();
            }
        } catch (Exception e) {
            log.warn("查询环境监测数据失败: {}", e.getMessage());
            envInfo = new HashMap<>();
        }

        EnvironmentSection section = new EnvironmentSection();
        section.setEnvInfo(envInfo);
        section.setSprayRecords(new ArrayList<>());
        return section;
    }

    private AiAlertsSection buildAiAlertsSection(Date todayStartDate) {
        Map<String, Integer> typeStats = new HashMap<>();
        List<AiAlert> todayRecords = Collections.emptyList();

        try {
            List<Map<String, Object>> typeAndCount = aiAlertMapper.getAlertTypeAndCount();
            for (Map<String, Object> item : typeAndCount) {
                String type = item.get("alertType") != null ? item.get("alertType").toString() : "未知";
                Integer count = toInt(item.get("numb"));
                typeStats.put(type, count);
            }
        } catch (Exception e) {
            log.warn("查询 AI 报警类型统计失败: {}", e.getMessage());
        }

        try {
            todayRecords = aiAlertMapper.selectList(
                    new LambdaQueryWrapper<AiAlert>()
                            .ge(AiAlert::getAlertTime, todayStartDate)
                            .orderByDesc(AiAlert::getAlertTime)
                            .last("limit 50")
            );
        } catch (Exception e) {
            log.warn("查询今日 AI 报警记录失败: {}", e.getMessage());
        }

        AiAlertsSection section = new AiAlertsSection();
        section.setRecords(new ArrayList<>(todayRecords));
        section.setTypeStats(typeStats);
        return section;
    }

    private MonitorAlertsSection buildMonitorAlertsSection(Date todayStartDate) {
        List<MonitorPointAlert> todayAlerts;
        try {
            todayAlerts = monitorPointAlertMapper.selectList(
                    new LambdaQueryWrapper<MonitorPointAlert>()
                            .ge(MonitorPointAlert::getCreateTime, todayStartDate)
                            .orderByDesc(MonitorPointAlert::getCreateTime)
                            .last("limit 50")
            );
        } catch (Exception e) {
            log.warn("查询今日设备监测报警失败: {}", e.getMessage());
            todayAlerts = Collections.emptyList();
        }

        int warnCount = 0;
        int alarmCount = 0;
        int controlCount = 0;
        for (MonitorPointAlert alert : todayAlerts) {
            if ("0".equals(alert.getAlertType())) {
                warnCount++;
            } else if ("1".equals(alert.getAlertType())) {
                alarmCount++;
            } else if ("2".equals(alert.getAlertType())) {
                controlCount++;
            }
        }

        Map<String, Integer> alertLevelStats = new HashMap<>();
        alertLevelStats.put("预警", warnCount);
        alertLevelStats.put("警报", alarmCount);
        alertLevelStats.put("控制报警", controlCount);

        MonitorAlertsSection section = new MonitorAlertsSection();
        section.setRecords(new ArrayList<>(todayAlerts));
        section.setAlertLevelStats(alertLevelStats);
        return section;
    }

    private OfflineRecordsSection buildOfflineRecordsSection(Date todayStartDate) {
        List<EquipmentOffline> todayOffline;
        try {
            todayOffline = equipmentOfflineMapper.selectList(
                    new LambdaQueryWrapper<EquipmentOffline>()
                            .ge(EquipmentOffline::getMonitorTime, todayStartDate)
                            .orderByDesc(EquipmentOffline::getMonitorTime)
                            .last("limit 50")
            );
            if (todayOffline == null) todayOffline = Collections.emptyList();
        } catch (Exception e) {
            log.warn("查询今日上下线记录失败: {}", e.getMessage());
            todayOffline = Collections.emptyList();
        }

        OfflineRecordsSection section = new OfflineRecordsSection();
        section.setRecords(new ArrayList<>(todayOffline));
        return section;
    }

    private static Date nowDate() {
        return DateUtil.date();
    }

    private static Integer toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Float toFloat(BigDecimal value) {
        return value == null ? null : value.floatValue();
    }
}
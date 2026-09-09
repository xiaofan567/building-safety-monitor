package com.qst.smartbuildings.tcpserver.handle;

import cn.hutool.core.bean.BeanUtil;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.TowerData;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.TowerWorkInfo;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.mapper.TowerDataMapper;
import com.qst.smartbuildings.mapper.TowerInfoMapper;
import com.qst.smartbuildings.mapper.TowerWorkInfoMapper;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 塔机数据处理及报警实现
 *
 * <p>负责塔机（塔吊）设备上报数据的处理：将实时数据与作业数据分类入库，
 * 并根据实时数据中的力矩、风速判断是否触发预警/报警并生成报警记录（含报警合并与状态恢复）。</p>
 *
 * @author 智能建造团队
 */
@Slf4j
@Service
public class TowerDataProcessHandle extends DeviceDataProcessHandle<TowerInfo> {

    /** 时间格式化 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 报警合并时间窗口（分钟） */
    private static final long ALERT_MERGE_WINDOW_MINUTES = 15;

    /** 力矩预警系数（超过额定力矩的90%触发预警） */
    private static final BigDecimal MOMENT_WARN_RATE = new BigDecimal("0.9");

    /** 风速预警阈值（m/s） */
    private static final BigDecimal WIND_WARN_THRESHOLD = new BigDecimal("10.8");

    /** 风速报警阈值（m/s） */
    private static final BigDecimal WIND_ALARM_THRESHOLD = new BigDecimal("17.2");

    private final TowerInfoMapper towerInfoMapper;
    private final TowerDataMapper towerDataMapper;
    private final TowerWorkInfoMapper towerWorkInfoMapper;
    private final MonitorPointAlertMapper alertMapper;
    private final RedisClient redisClient;
    private final RestTemplate restTemplate;

    /** 塔机AI风险预测服务地址 */
    @Value("${tower.ai.baseUrl:}")
    private String aiBaseUrl;

    public TowerDataProcessHandle(TowerInfoMapper towerInfoMapper,
                                  TowerDataMapper towerDataMapper,
                                  TowerWorkInfoMapper towerWorkInfoMapper,
                                  MonitorPointAlertMapper alertMapper,
                                  RedisClient redisClient,
                                  RestTemplate restTemplate) {
        this.towerInfoMapper = towerInfoMapper;
        this.towerDataMapper = towerDataMapper;
        this.towerWorkInfoMapper = towerWorkInfoMapper;
        this.alertMapper = alertMapper;
        this.redisClient = redisClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 初始化塔机信息到内存缓存，以平台设备标识 platformEqCode 为 Key
     */
    @Override
    @PostConstruct
    public void init() {
        List<TowerInfo> towerList = towerInfoMapper.getTowerList();
        for (TowerInfo towerInfo : towerList) {
            if (towerInfo.getPlatformEqCode() != null) {
                dataMap.put(towerInfo.getPlatformEqCode(), towerInfo);
            }
        }
        log.info("塔机数据处理组件加载完成，共 {} 台塔机", dataMap.size());
    }

    /**
     * 处理塔机上报数据（通过线程池异步执行）
     *
     * <p>根据上报数据是否包含监测时间判断数据类型：包含则按实时数据处理（入库并校验报警），
     * 否则按作业（工作循环）数据处理。</p>
     */
    @Override
    @Async("DataProcessTaskExecutor")
    public void processData(DeviceReportDTO data, String clientId) {
        TowerInfo towerInfo = dataMap.get(data.getCode());
        if (towerInfo == null) {
            log.warn("塔机数据处理未匹配到塔机 code={}", data.getCode());
            return;
        }
        Map<String, String> properties = data.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        if (properties.containsKey("monitorTime")) {
            // 实时数据
            TowerData towerData = BeanUtil.toBean(properties, TowerData.class);
            if (towerData != null) {
                towerData.setAssetId(towerInfo.getAssetId());
                towerData.setMonitorTime(LocalDateTime.now());
                towerDataMapper.insert(towerData);
                // 力矩报警判断
                processMoment(towerData, towerInfo);
                // 风速报警判断
                processWind(towerData);
            }
            // TODO 任务5-2-2：通过塔机AI风险预测服务（/crane_safety）实现风险预测与AI报警，本任务暂不实现
            //lstmSafeCheck(towerData, towerInfo);
        } else {
            // 作业（工作循环）数据
            TowerWorkInfo workInfo = BeanUtil.toBean(properties, TowerWorkInfo.class);
            if (workInfo != null) {
                workInfo.setAssetId(towerInfo.getAssetId());
                workInfo.setCreateTime(LocalDateTime.now());
                towerWorkInfoMapper.insert(workInfo);
            }
        }
    }

    /**
     * 力矩报警判断：超过额定力矩（或按比例取整）触发报警/预警，回落后写入恢复值
     *
     * @param towerData 实时数据
     * @param towerInfo 塔机信息
     */
    private void processMoment(TowerData towerData, TowerInfo towerInfo) {
        BigDecimal moment = towerData.getMoment();
        BigDecimal ratedMoment = towerInfo.getRatedMoment();
        if (moment == null || ratedMoment == null || ratedMoment.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String alertKey = RedisConst.TOWER_MOMENT_HEAD + towerData.getAssetId();
        BigDecimal warnValue = ratedMoment.multiply(MOMENT_WARN_RATE);
        if (moment.compareTo(ratedMoment) > 0) {
            // 力矩超过额定力矩 -> 报警
            mergeOrCreateAlert(alertKey, towerData.getAssetId(), moment.toString(),
                    "力矩超限报警", "1");
        } else if (moment.compareTo(warnValue) > 0) {
            // 力矩超过额定力矩的90% -> 预警
            mergeOrCreateAlert(alertKey, towerData.getAssetId(), moment.toString(),
                    "力矩超限预警", "0");
        } else {
            // 正常，若之前存在报警则写入恢复值
            recoverAlert(alertKey, towerData.getAssetId(), moment.toString());
        }
    }

    /**
     * 风速报警判断：超过阈值触发报警/预警，回落后写入恢复值
     *
     * @param towerData 实时数据
     */
    private void processWind(TowerData towerData) {
        BigDecimal windSpeed = towerData.getWindSpeed();
        if (windSpeed == null) {
            return;
        }
        String alertKey = RedisConst.TOWER_WIND_HEAD + towerData.getAssetId();
        if (windSpeed.compareTo(WIND_ALARM_THRESHOLD) > 0) {
            // 风速超过报警阈值
            mergeOrCreateAlert(alertKey, towerData.getAssetId(), windSpeed.toString(),
                    "风速超限报警", "1");
        } else if (windSpeed.compareTo(WIND_WARN_THRESHOLD) > 0) {
            // 风速超过预警阈值
            mergeOrCreateAlert(alertKey, towerData.getAssetId(), windSpeed.toString(),
                    "风速超限预警", "0");
        } else {
            // 正常，若之前存在报警则写入恢复值
            recoverAlert(alertKey, towerData.getAssetId(), windSpeed.toString());
        }
    }

    /**
     * 通用报警事件处理（含事件合并），镜像设备资产统一的报警逻辑
     *
     * @param alertKey    Redis报警缓存Key
     * @param assetId     设备ID
     * @param monitorData 触发报警的有效值
     * @param content     报警内容
     * @param alertType   提醒类型（0预警，1报警）
     */
    private void mergeOrCreateAlert(String alertKey, Long assetId, String monitorData,
                                    String content, String alertType) {
        Object o = redisClient.get(alertKey);
        if (o instanceof Number) {
            // 之前存在报警事件，进行合并
            Long paId = ((Number) o).longValue();
            MonitorPointAlert alert = alertMapper.selectById(paId);
            if (alert != null && "0".equals(alert.getHandleState())) {
                alert.setMonitorLog(String.format("%s\r\n 报警时间:%s 报警值:%s;",
                        alert.getMonitorLog(), LocalDateTime.now().format(TIME_FORMATTER), monitorData));
                alertMapper.updateById(alert);
                log.info("塔机报警合并 id={} 内容={} 值={}", alert.getId(), content, monitorData);
            } else {
                // 已处理，删除缓存，后续重新触发则新建报警
                redisClient.delete(alertKey);
            }
        } else {
            // 之前无报警事件，创建报警记录
            MonitorPointAlert alert = new MonitorPointAlert();
            alert.setEquipmentId(assetId);
            alert.setMonitorData(monitorData);
            alert.setContent(content);
            alert.setMonitorLog(String.format("报警时间:%s 报警值:%s;",
                    LocalDateTime.now().format(TIME_FORMATTER), monitorData));
            alert.setAlertType(alertType);
            alert.setMonitorAlertType("2");
            alert.setHandleState("0");
            alert.setCreateTime(LocalDateTime.now());
            alertMapper.insert(alert);
            redisClient.set(alertKey, alert.getId(), ALERT_MERGE_WINDOW_MINUTES, TimeUnit.MINUTES);
            log.info("创建塔机报警 id={} 内容={} 值={}", alert.getId(), content, monitorData);
        }
    }

    /**
     * 报警恢复处理：当数据回落到正常范围，若之前存在未处理的报警记录则写入恢复值并清除缓存
     *
     * @param alertKey     Redis报警缓存Key
     * @param assetId      设备ID
     * @param recoveryData 恢复时的监测值
     */
    private void recoverAlert(String alertKey, Long assetId, String recoveryData) {
        Object o = redisClient.get(alertKey);
        if (o instanceof Number) {
            Long paId = ((Number) o).longValue();
            MonitorPointAlert alert = alertMapper.selectById(paId);
            if (alert != null) {
                alert.setRecoveryData(recoveryData);
                alert.setRecoveryTime(LocalDateTime.now());
                alertMapper.updateById(alert);
                redisClient.delete(alertKey);
                log.info("塔机报警恢复 id={} 恢复值={}", alert.getId(), recoveryData);
            }
        }
    }
}
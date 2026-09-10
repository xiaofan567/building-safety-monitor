package com.qst.smartbuildings.tcpserver.handle;

import cn.hutool.core.bean.BeanUtil;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.ElevatorData;
import com.qst.smartbuildings.entity.ElevatorInfo;
import com.qst.smartbuildings.entity.ElevatorWorkInfo;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.mapper.ElevatorDataMapper;
import com.qst.smartbuildings.mapper.ElevatorInfoMapper;
import com.qst.smartbuildings.mapper.ElevatorWorkInfoMapper;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 升降机（施工电梯）数据处理及报警实现
 *
 * <p>负责升降机设备上报数据的处理：将实时数据与作业数据分类入库，
 * 并根据实时数据中的重量、速度、倾角判断是否触发预警/报警并生成报警记录（含报警合并与状态恢复）。</p>
 *
 * @author 智能建造团队
 */
@Slf4j
@Service
public class ElevatorInfoDataProcessHandle extends DeviceDataProcessHandle<ElevatorInfo> {

    /** 时间格式化 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 报警合并时间窗口（分钟） */
    private static final long ALERT_MERGE_WINDOW_MINUTES = 15;

    /** 超重预警系数（超过额定重量的90%触发预警） */
    private static final double OVER_WEIGHT_WARN_RATE = 0.9;

    /** 超重报警系数（超过额定重量的110%触发报警） */
    private static final double OVER_WEIGHT_ALARM_RATE = 1.1;

    /** 超速预警系数（超过额定速度的90%触发预警） */
    private static final double OVER_SPEED_WARN_RATE = 0.9;

    /** 超速报警系数（超过额定速度的110%触发报警） */
    private static final double OVER_SPEED_ALARM_RATE = 1.1;

    /** 倾角预警阈值（超过5度触发预警） */
    private static final double ANGLE_WARN_THRESHOLD = 5.0;

    /** 倾角报警阈值（超过8度触发报警） */
    private static final double ANGLE_ALARM_THRESHOLD = 8.0;

    private final ElevatorInfoMapper elevatorInfoMapper;
    private final ElevatorDataMapper elevatorDataMapper;
    private final ElevatorWorkInfoMapper elevatorWorkInfoMapper;
    private final MonitorPointAlertMapper alertMapper;
    private final RedisClient redisClient;

    public ElevatorInfoDataProcessHandle(ElevatorInfoMapper elevatorInfoMapper,
                                        ElevatorDataMapper elevatorDataMapper,
                                        ElevatorWorkInfoMapper elevatorWorkInfoMapper,
                                        MonitorPointAlertMapper alertMapper,
                                        RedisClient redisClient) {
        this.elevatorInfoMapper = elevatorInfoMapper;
        this.elevatorDataMapper = elevatorDataMapper;
        this.elevatorWorkInfoMapper = elevatorWorkInfoMapper;
        this.alertMapper = alertMapper;
        this.redisClient = redisClient;
    }

    /**
     * 初始化升降机信息到内存缓存，以平台设备标识 platformEqCode 为 Key
     */
    @Override
    @PostConstruct
    public void init() {
        List<ElevatorInfo> elevatorList = elevatorInfoMapper.getAllElevator();
        for (ElevatorInfo elevator : elevatorList) {
            if (elevator.getPlatformEqCode() != null) {
                dataMap.put(elevator.getPlatformEqCode(), elevator);
            }
        }
        log.info("升降机数据处理组件加载完成，共 {} 台升降机", dataMap.size());
    }

    /**
     * 处理升降机上报数据（通过线程池异步执行）
     *
     * <p>根据上报数据是否包含监测时间判断数据类型：包含则按实时数据处理（入库并校验报警），
     * 否则按作业（工作循环）数据处理。</p>
     */
    @Override
    @Async("DataProcessTaskExecutor")
    public void processData(DeviceReportDTO data, String clientId) {
        ElevatorInfo elevator = dataMap.get(data.getCode());
        if (elevator == null) {
            log.warn("升降机数据处理未匹配到升降机 code={}", data.getCode());
            return;
        }
        Map<String, String> properties = data.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        if (properties.containsKey("startTime")) {
            // 作业（工作循环）数据
            ElevatorWorkInfo workInfo = BeanUtil.toBean(properties, ElevatorWorkInfo.class);
            if (workInfo != null) {
                workInfo.setAssetId(elevator.getAssetId());
                elevatorWorkInfoMapper.insert(workInfo);
            }
        } else if (properties.containsKey("monitorTime")) {
            // 实时数据
            ElevatorData elevatorData = BeanUtil.toBean(properties, ElevatorData.class);
            if (elevatorData != null) {
                elevatorData.setAssetId(elevator.getAssetId());
                elevatorData.setMonitorTime(LocalDateTime.now());
                elevatorDataMapper.insert(elevatorData);
                // 检查超重、超速、角度偏差
                checkOverWeight(elevatorData, elevator);
                checkOverSpeed(elevatorData, elevator);
                checkAngleDeviation(elevatorData, elevator);
            }
        }
    }

    /**
     * 超重报警判断：超过额定重量90%预警、110%报警，回落后写入恢复值
     *
     * @param elevatorData 实时数据
     * @param elevatorInfo 升降机信息
     */
    public void checkOverWeight(ElevatorData elevatorData,ElevatorInfo elevatorInfo){
        if(elevatorData.getWeight()==null) return;
        double weight = elevatorData.getWeight();
        double ratedWeight = elevatorInfo.getRatedWeight();
        if (weight > ratedWeight * OVER_WEIGHT_WARN_RATE) {
            Object o = redisClient.get(RedisConst.ELEVATOR_OVER_WEIGHT_HEAD + elevatorData.getAssetId());
            if(o == null){
                MonitorPointAlert pointAlert = new MonitorPointAlert();
                pointAlert.setEquipmentId(elevatorData.getAssetId());
                pointAlert.setContent("超重预警");
                pointAlert.setCreateTime(LocalDateTime.now());
                pointAlert.setMonitorData(String.valueOf(elevatorData.getWeight()));
                pointAlert.setMonitorLog(String.format("超重预警：时间:%s，当前重量：%s，额定重量：%s",
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getWeight(), elevatorInfo.getRatedWeight()));
                pointAlert.setAlertType("0");
                pointAlert.setMonitorAlertType("3");
                if(elevatorData.getWeight() > ratedWeight * OVER_WEIGHT_ALARM_RATE){
                    pointAlert.setContent("超重报警");
                    pointAlert.setMonitorLog(String.format("超重报警：时间:%s，当前重量：%s，额定重量：%s",
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getWeight(), elevatorInfo.getRatedWeight()));
                    pointAlert.setAlertType("1");
                }
                pointAlert.setHandleState("0");
                alertMapper.insert(pointAlert);
                redisClient.set(RedisConst.ELEVATOR_OVER_WEIGHT_HEAD + elevatorData.getAssetId(), pointAlert.getId(), ALERT_MERGE_WINDOW_MINUTES, TimeUnit.MINUTES);
                log.info("创建升降机报警 id={} 内容={} 值={}", pointAlert.getId(), pointAlert.getContent(), elevatorData.getWeight());
            }else{
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                String s = String.format("%s\r\n超重预警：时间:%s，当前重量：%s，额定重量：%s",
                        (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getWeight(), elevatorInfo.getRatedWeight());
                if(elevatorData.getWeight() > ratedWeight * OVER_WEIGHT_ALARM_RATE){
                    pointAlert.setContent("超重报警");
                    s = String.format("%s\r\n超重报警：时间:%s，当前重量：%s，额定重量：%s",
                            (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getWeight(), elevatorInfo.getRatedWeight());
                    pointAlert.setAlertType("1");
                }
                pointAlert.setMonitorLog(s);
                alertMapper.updateById(pointAlert);
                log.info("升降机报警合并 id={} 内容={} 值={}", pointAlert.getId(), pointAlert.getContent(), elevatorData.getWeight());
            }
        }else{
            Object o = redisClient.get(RedisConst.ELEVATOR_OVER_WEIGHT_HEAD + elevatorData.getAssetId());
            if(o != null){
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                if (pointAlert != null) {
                    pointAlert.setRecoveryData(String.valueOf(elevatorData.getWeight()));
                    pointAlert.setRecoveryTime(LocalDateTime.now());
                    alertMapper.updateById(pointAlert);
                    redisClient.delete(RedisConst.ELEVATOR_OVER_WEIGHT_HEAD + elevatorData.getAssetId());
                    log.info("升降机报警恢复 id={} 恢复值={}", pointAlert.getId(), elevatorData.getWeight());
                }
            }
        }
    }

    /**
     * 超速报警判断：超过额定速度90%预警、110%报警，回落后写入恢复值
     *
     * @param elevatorData 实时数据
     * @param elevatorInfo 升降机信息
     */
    public void checkOverSpeed(ElevatorData elevatorData,ElevatorInfo elevatorInfo){
        if(elevatorData.getSpeed()==null) return;
        double speed = elevatorData.getSpeed();
        double ratedSpeed = elevatorInfo.getAscendingSpeed();
        if(speed > ratedSpeed * OVER_SPEED_WARN_RATE){
            Object o = redisClient.get(RedisConst.ELEVATOR_OVER_SPEED_HEAD + elevatorData.getAssetId());
            if(o == null){
                MonitorPointAlert pointAlert = new MonitorPointAlert();
                pointAlert.setEquipmentId(elevatorData.getAssetId());
                pointAlert.setContent("超速预警");
                pointAlert.setCreateTime(LocalDateTime.now());
                pointAlert.setMonitorData(String.valueOf(elevatorData.getSpeed()));
                pointAlert.setMonitorLog(String.format("超速预警：时间:%s，当前速度：%s，额定起升速度：%s",
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getSpeed(), elevatorInfo.getAscendingSpeed()));
                pointAlert.setAlertType("0");
                pointAlert.setMonitorAlertType("3");
                if(speed > ratedSpeed * OVER_SPEED_ALARM_RATE){
                    pointAlert.setContent("超速报警");
                    pointAlert.setMonitorLog(String.format("超速报警：时间:%s，当前速度：%s，额定起升速度：%s",
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getSpeed(), elevatorInfo.getAscendingSpeed()));
                    pointAlert.setAlertType("1");
                }
                pointAlert.setHandleState("0");
                alertMapper.insert(pointAlert);
                redisClient.set(RedisConst.ELEVATOR_OVER_SPEED_HEAD + elevatorData.getAssetId(), pointAlert.getId(), ALERT_MERGE_WINDOW_MINUTES, TimeUnit.MINUTES);
                log.info("创建升降机报警 id={} 内容={} 值={}", pointAlert.getId(), pointAlert.getContent(), elevatorData.getSpeed());
            }else{
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                String s = String.format("%s\r\n超速预警：时间:%s，当前速度：%s，额定起升速度：%s",
                        (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getSpeed(), elevatorInfo.getAscendingSpeed());
                if(speed > ratedSpeed * OVER_SPEED_ALARM_RATE){
                    pointAlert.setContent("超速报警");
                    s = String.format("%s\r\n超速报警：时间:%s，当前速度：%s，额定起升速度：%s",
                            (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), elevatorData.getSpeed(), elevatorInfo.getAscendingSpeed());
                    pointAlert.setAlertType("1");
                }
                pointAlert.setMonitorLog(s);
                alertMapper.updateById(pointAlert);
                log.info("升降机报警合并 id={} 内容={} 值={}", pointAlert.getId(), pointAlert.getContent(), elevatorData.getSpeed());
            }
        }else{
            Object o = redisClient.get(RedisConst.ELEVATOR_OVER_SPEED_HEAD + elevatorData.getAssetId());
            if(o != null){
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                if (pointAlert != null) {
                    pointAlert.setRecoveryData(String.valueOf(elevatorData.getSpeed()));
                    pointAlert.setRecoveryTime(LocalDateTime.now());
                    alertMapper.updateById(pointAlert);
                    redisClient.delete(RedisConst.ELEVATOR_OVER_SPEED_HEAD + elevatorData.getAssetId());
                    log.info("升降机报警恢复 id={} 恢复值={}", pointAlert.getId(), elevatorData.getSpeed());
                }
            }
        }
    }

    /**
     * 角度偏差报警判断：x或y轴倾角绝对值超过5度预警、超过8度报警，回落后写入恢复值
     *
     * @param elevatorData 实时数据
     * @param elevatorInfo 升降机信息
     */
    public void checkAngleDeviation(ElevatorData elevatorData,ElevatorInfo elevatorInfo){
        if(elevatorData.getXAngle()==null || elevatorData.getYAngle()==null) return;
        double x = elevatorData.getXAngle();
        double y = elevatorData.getYAngle();
        if(Math.abs(x) > ANGLE_WARN_THRESHOLD || Math.abs(y) > ANGLE_WARN_THRESHOLD){
            Object o = redisClient.get(RedisConst.ELEVATOR_ANGLE_HEAD+elevatorData.getAssetId());
            if(o == null){
                MonitorPointAlert pointAlert = new MonitorPointAlert();
                pointAlert.setEquipmentId(elevatorData.getAssetId());
                pointAlert.setContent("角度偏差预警");
                pointAlert.setCreateTime(LocalDateTime.now());
                pointAlert.setMonitorData(String.format("x轴角度：%s，y轴角度：%s", x, y));
                pointAlert.setMonitorLog(String.format("角度偏差预警：时间:%s，x轴角度：%s，y轴角度：%s",
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), x, y));
                pointAlert.setAlertType("0");
                pointAlert.setMonitorAlertType("3");
                if(Math.abs(x) > ANGLE_ALARM_THRESHOLD || Math.abs(y) > ANGLE_ALARM_THRESHOLD){
                    pointAlert.setContent("角度偏差报警");
                    pointAlert.setMonitorLog(String.format("角度偏差报警：时间:%s，x轴角度：%s，y轴角度：%s",
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), x, y));
                    pointAlert.setAlertType("1");
                }
                pointAlert.setHandleState("0");
                alertMapper.insert(pointAlert);
                redisClient.set(RedisConst.ELEVATOR_ANGLE_HEAD+elevatorData.getAssetId(), pointAlert.getId(), ALERT_MERGE_WINDOW_MINUTES, TimeUnit.MINUTES);
                log.info("创建升降机报警 id={} 内容={} x:{} y:{}", pointAlert.getId(), pointAlert.getContent(), x, y);
            }else{
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                String s = String.format("%s\r\n角度偏差预警：时间:%s，x轴角度：%s，y轴角度：%s",
                        (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                        elevatorData.getMonitorTime().format(TIME_FORMATTER), x, y);
                if(Math.abs(x) > ANGLE_ALARM_THRESHOLD || Math.abs(y) > ANGLE_ALARM_THRESHOLD){
                    pointAlert.setContent("角度偏差报警");
                    s = String.format("%s\r\n角度偏差报警：时间:%s，x轴角度：%s，y轴角度：%s",
                            (pointAlert.getMonitorLog() == null ? "" : pointAlert.getMonitorLog()),
                            elevatorData.getMonitorTime().format(TIME_FORMATTER), x, y);
                    pointAlert.setAlertType("1");
                }
                pointAlert.setMonitorLog(s);
                alertMapper.updateById(pointAlert);
                log.info("升降机报警合并 id={} 内容={} x:{} y:{}", pointAlert.getId(), pointAlert.getContent(), x, y);
            }
        }else{
            Object o = redisClient.get(RedisConst.ELEVATOR_ANGLE_HEAD + elevatorData.getAssetId());
            if(o != null){
                Long paId = ((Number) o).longValue();
                MonitorPointAlert pointAlert = alertMapper.selectById(paId);
                if (pointAlert != null) {
                    pointAlert.setRecoveryData(String.format("x轴：%s，y轴：%s", x, y));
                    pointAlert.setRecoveryTime(LocalDateTime.now());
                    alertMapper.updateById(pointAlert);
                    redisClient.delete(RedisConst.ELEVATOR_ANGLE_HEAD + elevatorData.getAssetId());
                    log.info("升降机报警恢复 id={} x:{} y:{}", pointAlert.getId(), x, y);
                }
            }
        }
    }
}

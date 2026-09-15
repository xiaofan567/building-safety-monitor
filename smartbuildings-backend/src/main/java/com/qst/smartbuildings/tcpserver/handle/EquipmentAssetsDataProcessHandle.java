package com.qst.smartbuildings.tcpserver.handle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.common.UpdateOperationEnum;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;
import com.qst.smartbuildings.entity.EquipmentOffline;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorValue;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentMonitorPointMapper;
import com.qst.smartbuildings.mapper.EquipmentOfflineMapper;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.mapper.MonitorPointValueMapper;
import com.qst.smartbuildings.service.DeviceDataService;
import com.qst.smartbuildings.tcpserver.SpraySystemDataProcessService;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 设备数据统一处理类
 *
 * <p>负责设备连接注册、上下线状态同步、设备数据入库，
 * 以及根据监测点配置规则进行数据检测与预警/报警处理（含报警合并）。</p>
 *
 * @author 智能建造团队
 */
@Slf4j
@Service
public class EquipmentAssetsDataProcessHandle {

    /** 时间格式化 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 报警合并时间窗口（分钟） */
    private static final long ALERT_MERGE_WINDOW_MINUTES = 15;

    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final EquipmentMonitorPointMapper equipmentMonitorPointMapper;
    private final EquipmentOfflineMapper equipmentOfflineMapper;
    private final MonitorPointValueMapper monitorPointValueMapper;
    private final MonitorPointAlertMapper alertMapper;
    private final DeviceDataService deviceDataService;
    private final RedisClient redisClient;
    private final SpraySystemDataProcessService spraySystemDataProcessService;

    // 设备资产Map，以平台设备标识platformEqCode为Key（用ConcurrentHashMap保证多线程安全）
    private Map<String, EquipmentAssets> equipmentAssetsMap;
    // 监测点Map，以设备ID为Key，Value为设备对应的监测点列表
    private Map<Long, List<EquipmentMonitorPoint>> equipmentMonitorPointMap;

    public EquipmentAssetsDataProcessHandle(EquipmentAssetsMapper equipmentAssetsMapper,
                                            EquipmentMonitorPointMapper equipmentMonitorPointMapper,
                                            EquipmentOfflineMapper equipmentOfflineMapper,
                                            MonitorPointValueMapper monitorPointValueMapper,
                                            MonitorPointAlertMapper alertMapper,
                                            DeviceDataService deviceDataService,
                                            RedisClient redisClient,
                                            SpraySystemDataProcessService spraySystemDataProcessService) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.equipmentMonitorPointMapper = equipmentMonitorPointMapper;
        this.equipmentOfflineMapper = equipmentOfflineMapper;
        this.monitorPointValueMapper = monitorPointValueMapper;
        this.alertMapper = alertMapper;
        this.deviceDataService = deviceDataService;
        this.redisClient = redisClient;
        this.spraySystemDataProcessService = spraySystemDataProcessService;
    }

    /**
     * 加载设备资产与监测点数据到内存
     */
    @PostConstruct
    public void loadAssetsAndMonitor() {
        // 加载设备资产（仅开启且配置了平台设备标识的设备）
        List<EquipmentAssets> equipmentAssetsList = equipmentAssetsMapper.selectList(
                new QueryWrapper<EquipmentAssets>()
                        .eq("open_status", "1")
                        .isNotNull("platform_eq_code"));
        // 加载设备监控点
        List<EquipmentMonitorPoint> equipmentMonitorPointList = equipmentMonitorPointMapper.selectList(
                new QueryWrapper<EquipmentMonitorPoint>().eq("status", "1"));
        // 初始化设备资产Map
        equipmentAssetsMap = new ConcurrentHashMap<>();
        for (EquipmentAssets equipmentAssets : equipmentAssetsList) {
            equipmentAssetsMap.put(equipmentAssets.getPlatformEqCode(), equipmentAssets);
        }
        // 初始化监测点Map
        equipmentMonitorPointMap = new ConcurrentHashMap<>();
        equipmentAssetsList.forEach(e -> equipmentMonitorPointMap.put(e.getAssetId(), new ArrayList<>()));
        for (EquipmentMonitorPoint equipmentMonitorPoint : equipmentMonitorPointList) {
            List<EquipmentMonitorPoint> monitorPoints = equipmentMonitorPointMap.get(equipmentMonitorPoint.getEquipmentId());
            // 如果设备列表不存在，则表示设备不存在或未开启，直接跳过
            if (monitorPoints == null) {
                continue;
            }
            monitorPoints.add(equipmentMonitorPoint);
        }
        log.info("设备数据解析服务加载完成：设备资产 {} 台，监测点 {} 个",
                equipmentAssetsMap.size(), equipmentMonitorPointList.size());
    }

    /**
     * 更新设备信息（新增、修改、删除），同步数据处理组件中的内存缓存
     *
     * <p>更新时存在状态变更的情况：开-&gt;开走更新、开-&gt;关走删除、关-&gt;开走新增、关-&gt;关直接返回。</p>
     *
     * @param equipmentAssets 设备资产信息
     * @param type            操作类型（add：新增，update：修改，delete：删除）
     */
    public void updateEquipmentAssets(EquipmentAssets equipmentAssets, UpdateOperationEnum type) {
        if (equipmentAssets == null || equipmentAssets.getAssetId() == null || equipmentAssets.getPlatformEqCode() == null) {
            return;
        }
        if (type == UpdateOperationEnum.UPDATE) {
            // 根据内存Map中是否存在该设备，判断设备之前的开关状态
            List<EquipmentMonitorPoint> points = equipmentMonitorPointMap.get(equipmentAssets.getAssetId());
            if (points != null) {
                // 之前为开启状态
                if ("0".equals(equipmentAssets.getOpenStatus())) {
                    // 开->关：走删除操作
                    type = UpdateOperationEnum.DELETE;
                }
                // 开->开：保持更新操作
            } else {
                // 之前为关闭状态
                if ("1".equals(equipmentAssets.getOpenStatus())) {
                    // 关->开：走新增操作
                    type = UpdateOperationEnum.ADD;
                } else {
                    // 关->关：直接返回
                    return;
                }
            }
        }
        if (type == UpdateOperationEnum.ADD) {
            equipmentAssetsMap.put(equipmentAssets.getPlatformEqCode(), equipmentAssets);
            equipmentMonitorPointMap.computeIfAbsent(equipmentAssets.getAssetId(), k -> new ArrayList<>());
        } else if (type == UpdateOperationEnum.UPDATE) {
            equipmentAssetsMap.put(equipmentAssets.getPlatformEqCode(), equipmentAssets);
        } else if (type == UpdateOperationEnum.DELETE) {
            equipmentAssetsMap.remove(equipmentAssets.getPlatformEqCode());
            equipmentMonitorPointMap.remove(equipmentAssets.getAssetId());
        }
        log.info("设备数据同步完成 assetId={} type={}", equipmentAssets.getAssetId(), type);
    }

    /**
     * 更新设备监测点信息，同步数据处理组件中的内存缓存
     *
     * @param equipmentMonitorPoint 监测点信息
     * @param type                  操作类型（add：新增，update：修改，delete：删除）
     */
    public void updateEquipmentMonitorPoint(EquipmentMonitorPoint equipmentMonitorPoint, UpdateOperationEnum type) {
        if (equipmentMonitorPoint == null || equipmentMonitorPoint.getId() == null || equipmentMonitorPoint.getEquipmentId() == null) {
            return;
        }
        List<EquipmentMonitorPoint> points = equipmentMonitorPointMap.get(equipmentMonitorPoint.getEquipmentId());
        if (type == UpdateOperationEnum.UPDATE) {
            if (points != null) {
                // 设备已开启：状态为启用时走新增（覆盖）操作，否则从内存中移除
                if ("1".equals(equipmentMonitorPoint.getStatus())) {
                    type = UpdateOperationEnum.ADD;
                } else {
                    type = UpdateOperationEnum.DELETE;
                }
            } else {
                // 设备未开启，无需同步
                return;
            }
        }
        if (type == UpdateOperationEnum.ADD) {
            List<EquipmentMonitorPoint> pointList = equipmentMonitorPointMap
                    .computeIfAbsent(equipmentMonitorPoint.getEquipmentId(), k -> new ArrayList<>());
            pointList.removeIf(p -> p.getId().equals(equipmentMonitorPoint.getId()));
            pointList.add(equipmentMonitorPoint);
        } else if (type == UpdateOperationEnum.DELETE) {
            if (points != null) {
                points.removeIf(p -> p.getId().equals(equipmentMonitorPoint.getId()));
            }
        }
        log.info("监测点数据同步完成 pointId={} type={}", equipmentMonitorPoint.getId(), type);
    }

    /**
     * 处理设备上报数据（通过线程池异步执行，提高多设备并发处理能力）
     */
    @Async("DataProcessTaskExecutor")
    public void processEquipmentAssetsData(DeviceReportDTO data, String clientId) {
        // 根据DTO设置上线状态，并获取资产对象
        EquipmentAssets assets = deviceOnline(data, clientId);
        // 如果不存在，则直接返回
        if (assets == null) {
            return;
        }
        // 进行数据处理，根据监测点规则进行数据监测
        List<EquipmentMonitorPoint> points = equipmentMonitorPointMap.get(assets.getAssetId());
        if (points != null) {
            for (EquipmentMonitorPoint point : points) {
                processPoint(data, point);
            }
        }
        // 调用服务层处理并存储数据
        deviceDataService.processDeviceData(data);
    }

    /**
     * 根据设备上报数据与监测点设置进行数据检测
     *
     * <p>匹配到监测点数据后保存监测值，并按配置的时间窗口进行统计计算（实时/平均/最大/最小/总和），
     * 最后进行阈值判别。</p>
     */
    private void processPoint(DeviceReportDTO data, EquipmentMonitorPoint point) {
        // 根据DTO中的数据与监测点数据是否匹配，判断是否需要进行数据处理
        String s = data.getProperties().get(point.getDeviceCode());
        if (s == null) {
            return;
        }
        // 保存监测点数据
        MonitorValue pointValue = new MonitorValue();
        pointValue.setEquipmentId(point.getEquipmentId());
        pointValue.setMonitorPointId(point.getId());
        pointValue.setMonitorData(s);
        pointValue.setMonitorTime(LocalDateTime.now());
        monitorPointValueMapper.insert(pointValue);
        // 数值预警处理，先判断是否为数字
        if (!s.matches("-?\\d+(\\.\\d+)?")) {
            return;
        }
        Double dataValue = Double.valueOf(s);
        // 监测点数据越界时，异步控制关联位置的喷淋设备
        spraySystemDataProcessService.processSpraySystemData(point, dataValue);
        // 如果检测时间为0，则直接进行数据处理
        if (point.getRangeTime() == null || point.getRangeTime() == 0) {
            checkPoint(point, dataValue);
            return;
        }
        // 如果时间不为0，则需要先计算统计值，再存入Redis。
        // 为避免先存后取可能取不到数据的情况，先查出已有数据计算，再将新数据一并处理。
        Set<String> set = redisClient.getKeysByPrefix(String.format("%s%d_", RedisConst.PROPERTY_NAME_HEAD, point.getId()));
        String statisticalType = point.getStatisticalType();
        if (statisticalType != null) {
            switch (statisticalType) {
                case "1":
                    // 平均值计算：窗口内所有数据（含当前值）取平均
                    double sum = 0;
                    int count = 0;
                    for (String key : set) {
                        Object o = redisClient.get(key);
                        if (o instanceof Number) {
                            sum += ((Number) o).doubleValue();
                            count++;
                        }
                    }
                    dataValue = (sum + dataValue) / (count + 1);
                    break;
                case "2":
                    // 最大值计算
                    for (String key : set) {
                        Object o = redisClient.get(key);
                        if (o instanceof Number) {
                            dataValue = Math.max(dataValue, ((Number) o).doubleValue());
                        }
                    }
                    break;
                case "3":
                    // 最小值计算
                    for (String key : set) {
                        Object o = redisClient.get(key);
                        if (o instanceof Number) {
                            dataValue = Math.min(dataValue, ((Number) o).doubleValue());
                        }
                    }
                    break;
                case "4":
                    // 总和计算
                    for (String key : set) {
                        Object o = redisClient.get(key);
                        if (o instanceof Number) {
                            dataValue += ((Number) o).doubleValue();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        // 将本次统计值存入Redis，过期时间为检测时间窗口
        redisClient.set(String.format("%s%d_%s", RedisConst.PROPERTY_NAME_HEAD, point.getId(), UUID.randomUUID()),
                dataValue, point.getRangeTime(), TimeUnit.SECONDS);
        log.info("监测点id为{}的监测数据为{}", point.getId(), dataValue);
        checkPoint(point, dataValue);
    }

    /**
     * 监测数据是否超出预警值或者报警值的范围
     *
     * <p>优先判断报警阈值，再判断预警阈值；全部未超出则为正常状态，
     * 若之前存在报警记录则写入恢复值。</p>
     */
    private void checkPoint(EquipmentMonitorPoint point, Double value) {
        if (point.getAlarmUpper() != null && value > point.getAlarmUpper()) {
            // 如果超出报警上限，则发送报警消息
            sendAlarmMessage(point, value, 1);
        } else if (point.getAlarmLower() != null && value.compareTo(point.getAlarmLower().doubleValue()) < 0) {
            // 如果超出报警下限，则发送报警消息
            sendAlarmMessage(point, value, 2);
        } else if (point.getWarnUpper() != null && value.compareTo(point.getWarnUpper().doubleValue()) > 0) {
            // 如果超出预警上限，则发送预警消息
            sendAlarmMessage(point, value, 3);
        } else if (point.getWarnLower() != null && value.compareTo(point.getWarnLower().doubleValue()) < 0) {
            // 如果超出预警下限，则发送预警消息
            sendAlarmMessage(point, value, 4);
        } else {
            // 如果都没有超出，则说明当前正常，可以是一直正常，也可以是从报警中恢复过来的。
            // 如果查询到之前的报警信息，则添加恢复值，并更新恢复时间
            Object o = redisClient.get(String.format("%s%d", RedisConst.MONITOR_POINT_NAME_HEAD, point.getId()));
            if (o instanceof Number) {
                Long paId = ((Number) o).longValue();
                MonitorPointAlert alert = alertMapper.selectById(paId);
                if (alert != null) {
                    alert.setRecoveryData(value.toString());
                    alert.setRecoveryTime(LocalDateTime.now());
                    alertMapper.updateById(alert);
                    redisClient.delete(String.format("%s%d", RedisConst.MONITOR_POINT_NAME_HEAD, point.getId()));
                    log.info("监测点id为{}报警恢复，恢复值：{}", point.getId(), value);
                }
            }
        }
    }

    /**
     * 通用报警事件处理，需要考虑事件合并，否则可能会出现许多条报警事件
     *
     * @param point 监测点
     * @param value 触发报警的有效值
     * @param i     报警类型（1超上限报警、2超下限报警、3超上限预警、4超下限预警）
     */
    private void sendAlarmMessage(EquipmentMonitorPoint point, Double value, int i) {
        log.info("发送报警消息，报警类型：{}，报警值：{}，报警点：{}", i, value, point);
        String alertKey = String.format("%s%d", RedisConst.MONITOR_POINT_NAME_HEAD, point.getId());
        // 如果之前有报警事件，则需要合并报警事件
        Object o = redisClient.get(alertKey);
        if (o instanceof Number) {
            Long paId = ((Number) o).longValue();
            MonitorPointAlert alert = alertMapper.selectById(paId);
            if (alert != null && "0".equals(alert.getHandleState())) {
                // 如果之前有报警事件，且事件未被处理，则需要合并报警事件
                alert.setMonitorLog(String.format("%s\r\n 报警时间:%s 报警值:%s;",
                        alert.getMonitorLog(), LocalDateTime.now().format(TIME_FORMATTER), value));
                alertMapper.updateById(alert);
            } else {
                // 如果被处理，则删除redis中的缓存，后续如果报警，则重新再添加一个报警事件
                redisClient.delete(alertKey);
            }
        } else {
            // 如果之前没有报警事件，则需要创建报警事件
            String content = point.getPointName();
            if (i == 1) {
                content += "超上限报警";
            } else if (i == 2) {
                content += "超下限报警";
            } else if (i == 3) {
                content += "超上限预警";
            } else if (i == 4) {
                content += "超下限预警";
            }
            MonitorPointAlert pointAlert = new MonitorPointAlert();
            pointAlert.setMonitorPointId(point.getId());
            pointAlert.setEquipmentId(point.getEquipmentId());
            pointAlert.setMonitorData(value.toString());
            pointAlert.setContent(content);
            pointAlert.setMonitorLog(String.format("报警时间:%s 报警值:%s;",
                    LocalDateTime.now().format(TIME_FORMATTER), value));
            pointAlert.setCreateTime(LocalDateTime.now());
            pointAlert.setHandleState("0");
            pointAlert.setAlertType(i == 1 || i == 2 ? "1" : "0");
            pointAlert.setMonitorAlertType("1");
            alertMapper.insert(pointAlert);
            redisClient.set(alertKey, pointAlert.getId(), ALERT_MERGE_WINDOW_MINUTES, TimeUnit.MINUTES);
            log.info("创建报警记录 id={} 监测点={} 内容={}", pointAlert.getId(), point.getPointName(), content);
        }
    }

    /**
     * 设备连接，当设备连接时，添加clientId到Redis中
     */
    public void deviceConnect(String clientId) {
        // 添加设备连接记录（0表示该客户端还未匹配设备）
        redisClient.set(RedisConst.CLIENT_NAME_HEAD + clientId, 0);
        log.info("设备连接注册成功 clientId={}", clientId);
    }

    /**
     * 设备上线
     */
    public EquipmentAssets deviceOnline(DeviceReportDTO data, String clientId) {
        // 根据code获取设备资产
        EquipmentAssets assets = equipmentAssetsMap.get(data.getCode());
        // 如果没有获取到资产，则直接返回null
        if (assets == null) {
            log.warn("未匹配到设备资产 code={}", data.getCode());
            return null;
        }
        // 根据clientId获取设备ID（Redis中为0表示此TCP客户端未匹配到任何设备）
        Object o = redisClient.get(RedisConst.CLIENT_NAME_HEAD + clientId);
        if (o != null && o.equals(0)) {
            // 通过Redis进行设备匹配（双向关联）
            redisClient.set(RedisConst.ASSETS_NAME_HEAD + assets.getPlatformEqCode(), clientId);
            redisClient.set(RedisConst.CLIENT_NAME_HEAD + clientId, assets.getAssetId());
            // 添加设备上线记录
            EquipmentOffline equipmentOffline = new EquipmentOffline();
            equipmentOffline.setEquipmentId(assets.getAssetId());
            equipmentOffline.setStatus(1);
            equipmentOffline.setMonitorTime(LocalDateTime.now());
            equipmentOfflineMapper.insert(equipmentOffline);
            // 更新设备状态为在线
            if (assets.getOfflineStatus() != null && "0".equals(assets.getOfflineStatus())) {
                assets.setOfflineStatus("1");
                assets.setOfflineDate(LocalDateTime.now());
                equipmentAssetsMapper.updateById(assets);
            }
            log.info("设备上线 assetId={} assetName={} clientId={}",
                    assets.getAssetId(), assets.getAssetName(), clientId);
        }
        return assets;
    }

    /**
     * 设备下线
     */
    public void deviceOffline(String clientId) {
        // 根据clientId获取设备ID
        Object o = redisClient.get(RedisConst.CLIENT_NAME_HEAD + clientId);
        if (o instanceof Number) {
            Long assetId = ((Number) o).longValue();
            // 为保证设备数据的完整性，需要先根据设备ID查询设备资产对象
            EquipmentAssets assets = equipmentAssetsMapper.selectById(assetId);
            if (assets != null) {
                // 下线操作
                assets.setOfflineDate(LocalDateTime.now());
                assets.setOfflineStatus("0");
                equipmentAssetsMapper.updateById(assets);
                // 添加设备下线记录
                EquipmentOffline equipmentOffline = new EquipmentOffline();
                equipmentOffline.setEquipmentId(assets.getAssetId());
                equipmentOffline.setStatus(0);
                equipmentOffline.setMonitorTime(LocalDateTime.now());
                equipmentOfflineMapper.insert(equipmentOffline);
                redisClient.delete(RedisConst.ASSETS_NAME_HEAD + assets.getPlatformEqCode());
                log.info("设备下线 assetId={} assetName={} clientId={}",
                        assets.getAssetId(), assets.getAssetName(), clientId);
            }
        }
        // 删除Redis中的数据
        redisClient.delete(RedisConst.CLIENT_NAME_HEAD + clientId);
    }
}

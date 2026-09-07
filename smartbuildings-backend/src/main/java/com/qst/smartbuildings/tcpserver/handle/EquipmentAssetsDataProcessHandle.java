package com.qst.smartbuildings.tcpserver.handle;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;
import com.qst.smartbuildings.entity.EquipmentOffline;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentMonitorPointMapper;
import com.qst.smartbuildings.mapper.EquipmentOfflineMapper;
import com.qst.smartbuildings.service.DeviceDataService;
import com.qst.smartbuildings.util.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备数据统一处理类
 *
 * <p>负责设备连接注册、上下线状态同步与设备数据入库。</p>
 *
 * @author 智能建造团队
 */
@Slf4j
@Service
public class EquipmentAssetsDataProcessHandle {

    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final EquipmentMonitorPointMapper equipmentMonitorPointMapper;
    private final EquipmentOfflineMapper equipmentOfflineMapper;
    private final DeviceDataService deviceDataService;
    private final RedisClient redisClient;

    // 设备资产Map，以平台设备标识platformEqCode为Key（用ConcurrentHashMap保证多线程安全）
    private Map<String, EquipmentAssets> equipmentAssetsMap;
    // 监测点Map，以设备ID为Key，Value为设备对应的监测点列表
    private Map<Long, List<EquipmentMonitorPoint>> equipmentMonitorPointMap;

    public EquipmentAssetsDataProcessHandle(EquipmentAssetsMapper equipmentAssetsMapper,
                                            EquipmentMonitorPointMapper equipmentMonitorPointMapper,
                                            EquipmentOfflineMapper equipmentOfflineMapper,
                                            DeviceDataService deviceDataService,
                                            RedisClient redisClient) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.equipmentMonitorPointMapper = equipmentMonitorPointMapper;
        this.equipmentOfflineMapper = equipmentOfflineMapper;
        this.deviceDataService = deviceDataService;
        this.redisClient = redisClient;
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
        // 调用服务层处理并存储数据
        deviceDataService.processDeviceData(data);
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

package com.qst.smartbuildings.tcpserver;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smartbuildings.common.EquipmentTypeConst;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.environment.SpraySystemMapper;
import com.qst.smartbuildings.util.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监测点数据触发喷淋设备控制。
 */
@Slf4j
@Service
public class SpraySystemDataProcessService {

    private static final int OPEN = 1;
    private static final int CLOSE = 0;
    private static final int FAILED = 2;

    private final SpraySystemMapper spraySystemMapper;
    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final RedisClient redisClient;
    private final TCPClientPool tcpClientPool;

    public SpraySystemDataProcessService(SpraySystemMapper spraySystemMapper,
                                         EquipmentAssetsMapper equipmentAssetsMapper,
                                         RedisClient redisClient,
                                         TCPClientPool tcpClientPool) {
        this.spraySystemMapper = spraySystemMapper;
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.redisClient = redisClient;
        this.tcpClientPool = tcpClientPool;
    }

    /**
     * 根据监测点配置和实时监测值控制同位置的喷淋设备。
     *
     * @param point 监测点
     * @param value 实时监测值
     */
    @Async("DataProcessTaskExecutor")
    public void processSpraySystemData(EquipmentMonitorPoint point, Double value) {
        if (point == null || value == null || !"1".equals(point.getIsSpraySystem())) {
            return;
        }
        if (point.getStartSprayValue() != null && value > point.getStartSprayValue()) {
            setSprinkler(point, OPEN);
        } else if (point.getCloseSprayValue() != null && value < point.getCloseSprayValue()) {
            setSprinkler(point, CLOSE);
        }
    }

    /**
     * 查询监测点设备所在位置的全部喷淋设备并下发控制指令。
     *
     * @param point  监测点
     * @param status 1：开启，0：关闭
     */
    private void setSprinkler(EquipmentMonitorPoint point, Integer status) {
        EquipmentAssets equipmentAssets = equipmentAssetsMapper.selectById(point.getEquipmentId());
        if (equipmentAssets == null || equipmentAssets.getLocationId() == null) {
            log.error("监测点{}关联设备不存在或未配置位置，无法触发喷淋", point.getId());
            return;
        }

        List<EquipmentAssets> sprinklerList = equipmentAssetsMapper.selectList(
                new LambdaQueryWrapper<EquipmentAssets>()
                        .eq(EquipmentAssets::getLocationId, equipmentAssets.getLocationId())
                        .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.SPRINKLER_TYPE_ID));
        if (sprinklerList == null || sprinklerList.isEmpty()) {
            log.error("设备id为{}的位置没有喷淋设备", equipmentAssets.getAssetId());
            return;
        }

        for (EquipmentAssets sprinkler : sprinklerList) {
            sendSprinkler(point, sprinkler, status);
        }
    }

    private void sendSprinkler(EquipmentMonitorPoint point, EquipmentAssets sprinkler, Integer status) {
        String clientId = getClientId(sprinkler);
        boolean success = clientId != null
                && tcpClientPool.sendMessage(clientId, buildControlMessage(status));
        if (!success) {
            log.error("喷淋设备{}的TCP连接不存在或发送失败", sprinkler.getAssetName());
        }
        spraySystemMapper.insert(buildSpraySystem(point, sprinkler,
                success ? status : FAILED));
    }

    private String getClientId(EquipmentAssets sprinkler) {
        if (sprinkler.getPlatformEqCode() == null) {
            return null;
        }
        Object clientId = redisClient.get(RedisConst.ASSETS_NAME_HEAD + sprinkler.getPlatformEqCode());
        return clientId == null ? null : clientId.toString();
    }

    private String buildControlMessage(Integer status) {
        return String.format("{status:control, message:%s, timestamp:%d}",
                status == OPEN ? "open" : "close", System.currentTimeMillis());
    }

    private SpraySystem buildSpraySystem(EquipmentMonitorPoint point,
                                         EquipmentAssets sprinkler,
                                         Integer status) {
        SpraySystem spraySystem = new SpraySystem();
        spraySystem.setEquipmentId(sprinkler.getAssetId().intValue());
        spraySystem.setLocationId(sprinkler.getLocationId().intValue());
        spraySystem.setMonitorPointId(point.getId().intValue());
        spraySystem.setTriggerType(1);
        spraySystem.setOperationType(status.toString());
        spraySystem.setOperationReason(buildOperationReason(sprinkler, status));
        spraySystem.setOperationTime(LocalDateTime.now());
        return spraySystem;
    }

    private String buildOperationReason(EquipmentAssets sprinkler, Integer status) {
        if (status == FAILED) {
            return String.format("喷淋执行失败，设备ID：%s，设备名：%s，原因：设备离线或TCP连接不可用",
                    sprinkler.getAssetId(), sprinkler.getAssetName());
        }
        String reason = status == OPEN ? "监测数据高于喷淋启动阈值" : "监测数据低于喷淋关闭阈值";
        return String.format("设备id为%s %s，%s喷淋操作",
                sprinkler.getAssetId(), reason, status == OPEN ? "开启" : "关闭");
    }
}

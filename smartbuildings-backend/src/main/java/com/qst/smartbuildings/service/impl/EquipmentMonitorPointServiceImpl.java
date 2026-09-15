package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentMonitorPoint;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentMonitorPointMapper;
import com.qst.smartbuildings.service.EquipmentMonitorPointService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 监测点信息服务实现
 *
 * @author 智能建造团队
 */
@Service
public class EquipmentMonitorPointServiceImpl
        extends ServiceImpl<EquipmentMonitorPointMapper, EquipmentMonitorPoint>
        implements EquipmentMonitorPointService {

    /** 默认状态：启用 */
    private static final String STATUS_ENABLED = "1";

    /** 默认统计类型 */
    private static final String STATISTICAL_TYPE_DEFAULT = "2";

    private final EquipmentAssetsMapper equipmentAssetsMapper;

    public EquipmentMonitorPointServiceImpl(EquipmentAssetsMapper equipmentAssetsMapper) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
    }

    @Override
    public BPage<EquipmentMonitorPoint> pageList(long current, long size, String monitorType, String pointName) {
        BPage<EquipmentMonitorPoint> pageData = new BPage<>(current, size);
        LambdaQueryWrapper<EquipmentMonitorPoint> wrapper = new LambdaQueryWrapper<EquipmentMonitorPoint>()
                .orderByAsc(EquipmentMonitorPoint::getSort);
        if (StringUtils.hasText(monitorType)) {
            wrapper.eq(EquipmentMonitorPoint::getMonitorType, monitorType);
        }
        if (StringUtils.hasText(pointName)) {
            wrapper.like(EquipmentMonitorPoint::getPointName, pointName);
        }
        this.page(pageData, wrapper);
        fillEquipmentNames(pageData.getRecords());
        return pageData;
    }

    @Override
    public EquipmentMonitorPoint getById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("监测点ID不能为空");
        }
        EquipmentMonitorPoint point = super.getById(id);
        if (point == null) {
            throw new IllegalArgumentException("监测点不存在");
        }
        fillEquipmentNames(List.of(point));
        return point;
    }

    @Override
    public void savePoint(EquipmentMonitorPoint equipmentMonitorPoint) {
        validate(equipmentMonitorPoint);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = currentUserId();
        if (equipmentMonitorPoint.getId() == null) {
            equipmentMonitorPoint.setCreateTime(now);
            equipmentMonitorPoint.setCreateUserId(currentUserId);
            this.save(equipmentMonitorPoint);
        } else {
            equipmentMonitorPoint.setUpdateTime(now);
            equipmentMonitorPoint.setUpdateUserId(currentUserId);
            this.updateById(equipmentMonitorPoint);
        }
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("监测点ID不能为空");
        }
        if (super.getById(id) == null) {
            throw new IllegalArgumentException("监测点不存在");
        }
        super.removeById(id);
    }

    /**
     * 批量补充设备名称（非表字段，用于接口返回）
     *
     * @param records 监测点列表
     */
    private void fillEquipmentNames(List<EquipmentMonitorPoint> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> equipmentIds = records.stream()
                .map(EquipmentMonitorPoint::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return;
        }
        Map<Long, String> equipmentNames = equipmentAssetsMapper
                .selectBatchIds(equipmentIds).stream()
                .collect(Collectors.toMap(EquipmentAssets::getAssetId,
                        EquipmentAssets::getAssetName, (a, b) -> a));
        records.forEach(record ->
                record.setEquipmentName(equipmentNames.get(record.getEquipmentId())));
    }

    /**
     * 获取当前登录用户 ID（来自认证过滤器写入的 SecurityContext）
     *
     * @return 用户 ID，未登录时返回 null
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 校验保存参数
     *
     * @param equipmentMonitorPoint 监测点信息
     */
    private void validate(EquipmentMonitorPoint equipmentMonitorPoint) {
        if (equipmentMonitorPoint == null) {
            throw new IllegalArgumentException("监测点信息不能为空");
        }
        if (!StringUtils.hasText(equipmentMonitorPoint.getPointName())) {
            throw new IllegalArgumentException("监测点名称不能为空");
        }
        if (equipmentMonitorPoint.getEquipmentId() == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        if (!StringUtils.hasText(equipmentMonitorPoint.getStatisticalType())) {
            equipmentMonitorPoint.setStatisticalType(STATISTICAL_TYPE_DEFAULT);
        }
        if (!StringUtils.hasText(equipmentMonitorPoint.getStatus())) {
            equipmentMonitorPoint.setStatus(STATUS_ENABLED);
        }
        if (!StringUtils.hasText(equipmentMonitorPoint.getIsSpraySystem())) {
            equipmentMonitorPoint.setIsSpraySystem("0");
        }
    }
}

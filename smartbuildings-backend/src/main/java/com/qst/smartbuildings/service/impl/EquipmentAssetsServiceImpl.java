package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentLocation;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentLocationMapper;
import com.qst.smartbuildings.mapper.EquipmentTypeMapper;
import com.qst.smartbuildings.service.EquipmentAssetsService;
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
 * 设备资产管理服务实现
 *
 * @author 智能建造团队
 */
@Service
public class EquipmentAssetsServiceImpl
        extends ServiceImpl<EquipmentAssetsMapper, EquipmentAssets>
        implements EquipmentAssetsService {

    /** 启用状态 */
    private static final String STATUS_ENABLED = "1";

    private final EquipmentLocationMapper equipmentLocationMapper;
    private final EquipmentTypeMapper equipmentTypeMapper;

    public EquipmentAssetsServiceImpl(EquipmentLocationMapper equipmentLocationMapper,
                                      EquipmentTypeMapper equipmentTypeMapper) {
        this.equipmentLocationMapper = equipmentLocationMapper;
        this.equipmentTypeMapper = equipmentTypeMapper;
    }

    @Override
    public BPage<EquipmentAssets> pageList(long current, long size) {
        BPage<EquipmentAssets> page = new BPage<>(current, size);
        this.page(page, new LambdaQueryWrapper<EquipmentAssets>()
                .eq(EquipmentAssets::getStatus, STATUS_ENABLED)
                .orderByAsc(EquipmentAssets::getSort));
        fillNames(page.getRecords());
        return page;
    }

    @Override
    public EquipmentAssets getById(Long assetId) {
        if (assetId == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        EquipmentAssets assets = super.getById(assetId);
        if (assets == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        fillNames(List.of(assets));
        return assets;
    }

    @Override
    public void saveAssets(EquipmentAssets equipmentAssets) {
        validate(equipmentAssets);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = currentUserId();
        if (equipmentAssets.getAssetId() == null) {
            equipmentAssets.setCreateTime(now);
            equipmentAssets.setCreateUserId(currentUserId);
            this.save(equipmentAssets);
        } else {
            equipmentAssets.setUpdateTime(now);
            equipmentAssets.setUpdateUserId(currentUserId);
            this.updateById(equipmentAssets);
        }
    }

    @Override
    public void delete(Long assetId) {
        if (assetId == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        if (super.getById(assetId) == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        super.removeById(assetId);
    }

    /**
     * 批量补充位置名称与类型名称（非表字段，用于接口返回）
     *
     * @param records 设备列表
     */
    private void fillNames(List<EquipmentAssets> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> locationIds = records.stream()
                .map(EquipmentAssets::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> typeIds = records.stream()
                .map(EquipmentAssets::getTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> locationNames = locationIds.isEmpty() ? Map.of()
                : equipmentLocationMapper.selectBatchIds(locationIds).stream()
                        .collect(Collectors.toMap(EquipmentLocation::getLocationId,
                                EquipmentLocation::getLocationName, (a, b) -> a));
        Map<Long, String> typeNames = typeIds.isEmpty() ? Map.of()
                : equipmentTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(EquipmentType::getTypeId,
                                EquipmentType::getTypeName, (a, b) -> a));

        records.forEach(record -> {
            record.setLocationName(locationNames.get(record.getLocationId()));
            record.setTypeName(typeNames.get(record.getTypeId()));
        });
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
     * @param equipmentAssets 设备信息
     */
    private void validate(EquipmentAssets equipmentAssets) {
        if (equipmentAssets == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        if (!StringUtils.hasText(equipmentAssets.getAssetCode())) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        if (!StringUtils.hasText(equipmentAssets.getAssetName())) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (equipmentAssets.getTypeId() == null) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (!StringUtils.hasText(equipmentAssets.getStatus())) {
            equipmentAssets.setStatus(STATUS_ENABLED);
        }
        if (equipmentAssets.getSort() == null) {
            equipmentAssets.setSort(0);
        }
    }
}

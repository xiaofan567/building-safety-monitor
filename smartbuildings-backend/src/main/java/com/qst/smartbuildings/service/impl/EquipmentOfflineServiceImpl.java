package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.dto.EquipmentOfflineQueryDTO;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentOffline;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentOfflineMapper;
import com.qst.smartbuildings.mapper.EquipmentTypeMapper;
import com.qst.smartbuildings.service.EquipmentOfflineService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备上下线记录服务实现
 *
 * @author 智能建造团队
 */
@Service
public class EquipmentOfflineServiceImpl
        extends ServiceImpl<EquipmentOfflineMapper, EquipmentOffline>
        implements EquipmentOfflineService {

    /** 默认当前页码 */
    private static final long DEFAULT_PAGE = 1L;

    /** 默认每页条数 */
    private static final long DEFAULT_LIMIT = 10L;

    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final EquipmentTypeMapper equipmentTypeMapper;

    public EquipmentOfflineServiceImpl(EquipmentAssetsMapper equipmentAssetsMapper,
                                       EquipmentTypeMapper equipmentTypeMapper) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.equipmentTypeMapper = equipmentTypeMapper;
    }

    @Override
    public BPage<EquipmentOffline> selectPage(EquipmentOfflineQueryDTO query) {
        long page = query == null || query.getPage() == null ? DEFAULT_PAGE : query.getPage();
        long limit = query == null || query.getLimit() == null ? DEFAULT_LIMIT : query.getLimit();
        BPage<EquipmentOffline> pageData = new BPage<>(page, limit);
        LambdaQueryWrapper<EquipmentOffline> wrapper = new LambdaQueryWrapper<EquipmentOffline>()
                .orderByDesc(EquipmentOffline::getId);
        if (query != null && query.getEquipmentId() != null) {
            wrapper.eq(EquipmentOffline::getEquipmentId, query.getEquipmentId());
        }
        this.page(pageData, wrapper);
        fillEquipmentInfo(pageData.getRecords());
        return pageData;
    }

    /**
     * 批量补充设备名称、设备编号、设备类型ID与类型名称（非表字段，用于接口返回）
     *
     * @param records 上下线记录列表
     */
    private void fillEquipmentInfo(List<EquipmentOffline> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> equipmentIds = records.stream()
                .map(EquipmentOffline::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return;
        }
        Map<Long, EquipmentAssets> assetMap = equipmentAssetsMapper
                .selectBatchIds(equipmentIds).stream()
                .collect(Collectors.toMap(EquipmentAssets::getAssetId,
                        a -> a, (a, b) -> a));

        Set<Long> typeIds = assetMap.values().stream()
                .map(EquipmentAssets::getTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> typeNames = typeIds.isEmpty() ? Collections.emptyMap()
                : equipmentTypeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(EquipmentType::getTypeId,
                                EquipmentType::getTypeName, (a, b) -> a));

        records.forEach(record -> {
            EquipmentAssets asset = assetMap.get(record.getEquipmentId());
            if (asset == null) {
                return;
            }
            record.setEquipmentName(asset.getAssetName());
            record.setAssetCode(asset.getAssetCode());
            record.setTypeId(asset.getTypeId() == null ? null : String.valueOf(asset.getTypeId()));
            record.setTypeName(typeNames.get(asset.getTypeId()));
        });
    }
}
package com.qst.smartbuildings.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.common.UpdateOperationEnum;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.ElevatorInfo;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.EquipmentLocation;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.mapper.CameraMapper;
import com.qst.smartbuildings.mapper.ElevatorInfoMapper;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.EquipmentLocationMapper;
import com.qst.smartbuildings.mapper.EquipmentTypeMapper;
import com.qst.smartbuildings.mapper.TowerInfoMapper;
import com.qst.smartbuildings.service.EquipmentAssetsService;
import com.qst.smartbuildings.tcpserver.handle.TowerDataProcessHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.qst.smartbuildings.common.EquipmentTypeConst.CAMERA_TYPE_ID;
import static com.qst.smartbuildings.common.EquipmentTypeConst.ELEVATOR_TYPE_ID;
import static com.qst.smartbuildings.common.EquipmentTypeConst.TOWER_TYPE_ID;

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


//新添加
    @Autowired
    private CameraMapper cameraMapper;
    @Autowired
    private TowerInfoMapper towerInfoMapper;
    @Autowired
    private ElevatorInfoMapper elevatorInfoMapper;
    @Autowired
    private TowerDataProcessHandle towerDataProcessHandle;
    /**
     * 重写保存方法
     * @param entity
     * @return
     */
    @Override
    public boolean save(EquipmentAssets entity) {
        boolean b = super.save(entity);
        if(b && entity.getTypeId() != null && entity.getTypeId().intValue() == CAMERA_TYPE_ID.intValue()){
            Camera camera = new Camera();
            camera.setAssetId(entity.getAssetId() == null ? null : entity.getAssetId().intValue());
            //默认状态为离线
            camera.setStatus("0");
            //默认启用
            camera.setIsEnable("1");
            //默认排序
            camera.setSort(0);
            cameraMapper.insert(camera);
        } else if (b && entity.getTypeId() != null && entity.getTypeId().intValue() == TOWER_TYPE_ID.intValue()) {
            // 新增塔机信息，并同步内存缓存
            TowerInfo towerInfo = new TowerInfo();
            BeanUtil.copyProperties(entity, towerInfo);
            if (StringUtils.hasText(entity.getExtraParams())) {
                TowerInfo ext = JSONUtil.toBean(entity.getExtraParams(), TowerInfo.class);
                towerInfo.setForwardArmLength(ext.getForwardArmLength());
                towerInfo.setBackwardArmLength(ext.getBackwardArmLength());
                towerInfo.setMaxHeight(ext.getMaxHeight());
                towerInfo.setRatedLoad(ext.getRatedLoad());
                towerInfo.setMaxLoad(ext.getMaxLoad());
                towerInfo.setRatedMoment(ext.getRatedMoment());
            }
            towerInfo.setAssetId(entity.getAssetId());
            towerInfo.setCreateTime(LocalDateTime.now());
            towerInfoMapper.insert(towerInfo);
            towerDataProcessHandle.update(towerInfo, entity.getPlatformEqCode(), UpdateOperationEnum.ADD);
        } else if (b && entity.getTypeId() != null && entity.getTypeId().intValue() == ELEVATOR_TYPE_ID.intValue()) {
            // 新增升降机信息
            ElevatorInfo elevatorInfo = new ElevatorInfo();
            elevatorInfo.setAssetId(entity.getAssetId());
            elevatorInfo.setAssetName(entity.getAssetName());
            elevatorInfoMapper.insert(elevatorInfo);
        }
        return b;
    }
    /*
     * 重写删除方法，后续有用
     * */
    @Override
    public boolean removeById(Serializable id) {
        EquipmentAssets equipmentAssets = getBaseMapper().selectById(id);
        boolean b = super.removeById(id);
        if(b && equipmentAssets != null && equipmentAssets.getTypeId() != null
                && equipmentAssets.getTypeId().intValue() == CAMERA_TYPE_ID.intValue()){
            cameraMapper.delete(new LambdaQueryWrapper<Camera>().eq(Camera::getAssetId, id));
        } else if (b && equipmentAssets != null && equipmentAssets.getTypeId() != null
                && equipmentAssets.getTypeId().intValue() == TOWER_TYPE_ID.intValue()) {
            // 删除塔机信息并同步内存缓存
            TowerInfo towerInfo = towerInfoMapper.selectOne(new LambdaQueryWrapper<TowerInfo>()
                    .eq(TowerInfo::getAssetId, id));
            if (towerInfo != null) {
                towerInfoMapper.deleteById(towerInfo.getId());
            }
            towerDataProcessHandle.update(null, equipmentAssets.getPlatformEqCode(), UpdateOperationEnum.DELETE);
        } else if (b && equipmentAssets != null && equipmentAssets.getTypeId() != null
                && equipmentAssets.getTypeId().intValue() == ELEVATOR_TYPE_ID.intValue()) {
            // 删除升降机信息
            elevatorInfoMapper.delete(new LambdaQueryWrapper<ElevatorInfo>().eq(ElevatorInfo::getAssetId, id));
        }
        return b;
    }

    /**
     * 重写更新方法，同步塔机/升降机信息变更到对应数据表与内存缓存
     *
     * @param entity 设备资产信息
     * @return 更新结果
     */
    @Override
    public boolean updateById(EquipmentAssets entity) {
        boolean b = super.updateById(entity);
        if (b && entity.getTypeId() != null && entity.getTypeId().intValue() == TOWER_TYPE_ID.intValue()) {
            // 更新塔机信息并同步内存缓存
            TowerInfo record = towerInfoMapper.selectOne(new LambdaQueryWrapper<TowerInfo>()
                    .eq(TowerInfo::getAssetId, entity.getAssetId()));
            if (record != null) {
                Long id = record.getId();
                record = new TowerInfo();
                BeanUtil.copyProperties(entity, record);
                if (StringUtils.hasText(entity.getExtraParams())) {
                    TowerInfo ext = JSONUtil.toBean(entity.getExtraParams(), TowerInfo.class);
                    record.setForwardArmLength(ext.getForwardArmLength());
                    record.setBackwardArmLength(ext.getBackwardArmLength());
                    record.setMaxHeight(ext.getMaxHeight());
                    record.setRatedLoad(ext.getRatedLoad());
                    record.setMaxLoad(ext.getMaxLoad());
                    record.setRatedMoment(ext.getRatedMoment());
                }
                record.setId(id);
                record.setAssetId(entity.getAssetId());
                towerInfoMapper.updateById(record);
                towerDataProcessHandle.update(record, entity.getPlatformEqCode(), UpdateOperationEnum.UPDATE);
            }
        } else if (b && entity.getTypeId() != null && entity.getTypeId().intValue() == ELEVATOR_TYPE_ID.intValue()) {
            // 更新升降机信息（参数由前端在升降机管理接口维护），此处仅同步不存在时兜底插入
            Long count = elevatorInfoMapper.selectCount(new LambdaQueryWrapper<ElevatorInfo>()
                    .eq(ElevatorInfo::getAssetId, entity.getAssetId()));
            if (count == null || count == 0) {
                ElevatorInfo elevatorInfo = new ElevatorInfo();
                elevatorInfo.setAssetId(entity.getAssetId());
                elevatorInfo.setAssetName(entity.getAssetName());
                elevatorInfoMapper.insert(elevatorInfo);
            }
        }
        return b;
    }
}

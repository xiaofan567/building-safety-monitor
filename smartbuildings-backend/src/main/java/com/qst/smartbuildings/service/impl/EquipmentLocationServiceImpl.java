package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.EquipmentLocation;
import com.qst.smartbuildings.mapper.EquipmentLocationMapper;
import com.qst.smartbuildings.service.EquipmentLocationService;
import com.qst.smartbuildings.vo.EquipmentLocationTreeVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备位置信息服务实现
 *
 * @author 智能建造团队
 */
@Service
public class EquipmentLocationServiceImpl
        extends ServiceImpl<EquipmentLocationMapper, EquipmentLocation>
        implements EquipmentLocationService {

    /** 根节点父级ID */
    private static final Long ROOT_PID = 0L;

    @Override
    public List<EquipmentLocationTreeVO> queryTreeList() {
        List<EquipmentLocation> all = this.list(
                new LambdaQueryWrapper<EquipmentLocation>()
                        .orderByAsc(EquipmentLocation::getSort));
        Map<Long, List<EquipmentLocationTreeVO>> childrenMap = all.stream()
                .map(this::toTreeVO)
                .collect(Collectors.groupingBy(EquipmentLocationTreeVO::getLocationPid));
        return buildTree(ROOT_PID, childrenMap);
    }

    @Override
    public EquipmentLocation getById(Long locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("位置ID不能为空");
        }
        EquipmentLocation location = super.getById(locationId);
        if (location == null) {
            throw new IllegalArgumentException("位置不存在");
        }
        return location;
    }

    @Override
    public void saveLocation(EquipmentLocation equipmentLocation) {
        validate(equipmentLocation);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = currentUserId();
        if (equipmentLocation.getLocationId() == null) {
            equipmentLocation.setCreateTime(now);
            equipmentLocation.setCreateUserId(currentUserId);
            this.save(equipmentLocation);
        } else {
            equipmentLocation.setUpdateTime(now);
            equipmentLocation.setUpdateUserId(currentUserId);
            this.updateById(equipmentLocation);
        }
    }

    @Override
    public void delete(Long locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("位置ID不能为空");
        }
        if (!super.removeById(locationId)) {
            throw new IllegalArgumentException("位置不存在");
        }
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
     * @param equipmentLocation 位置信息
     */
    private void validate(EquipmentLocation equipmentLocation) {
        if (equipmentLocation == null) {
            throw new IllegalArgumentException("位置信息不能为空");
        }
        if (!StringUtils.hasText(equipmentLocation.getLocationName())) {
            throw new IllegalArgumentException("位置名称不能为空");
        }
        if (equipmentLocation.getLocationPid() == null) {
            equipmentLocation.setLocationPid(ROOT_PID);
        }
        if (equipmentLocation.getSort() == null) {
            equipmentLocation.setSort(0);
        }
    }

    /**
     * 递归构建地址树，仅保留从根节点可达的节点
     *
     * @param pid         父级ID
     * @param childrenMap 父级ID -> 子节点列表
     * @return 地址树列表
     */
    private List<EquipmentLocationTreeVO> buildTree(Long pid,
                                                    Map<Long, List<EquipmentLocationTreeVO>> childrenMap) {
        List<EquipmentLocationTreeVO> children = childrenMap.get(pid);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        children.forEach(child -> child.setChildren(buildTree(child.getLocationId(), childrenMap)));
        return children;
    }

    /**
     * 实体转为树形 VO
     *
     * @param location 位置实体
     * @return 树形 VO
     */
    private EquipmentLocationTreeVO toTreeVO(EquipmentLocation location) {
        EquipmentLocationTreeVO vo = new EquipmentLocationTreeVO();
        vo.setLocationId(location.getLocationId());
        vo.setLocationCode(location.getLocationCode());
        vo.setLocationName(location.getLocationName());
        vo.setLocationPid(location.getLocationPid());
        vo.setSort(location.getSort());
        vo.setCreateTime(location.getCreateTime());
        vo.setCreateUserId(location.getCreateUserId());
        vo.setUpdateTime(location.getUpdateTime());
        vo.setUpdateUserId(location.getUpdateUserId());
        return vo;
    }
}
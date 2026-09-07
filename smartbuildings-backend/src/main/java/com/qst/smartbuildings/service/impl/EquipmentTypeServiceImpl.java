package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.mapper.EquipmentTypeMapper;
import com.qst.smartbuildings.service.EquipmentTypeService;
import com.qst.smartbuildings.vo.EquipmentTypeTreeVO;
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
 * 设备类型信息服务实现
 *
 * @author 智能建造团队
 */
@Service
public class EquipmentTypeServiceImpl
        extends ServiceImpl<EquipmentTypeMapper, EquipmentType>
        implements EquipmentTypeService {

    /** 根节点父级ID */
    private static final Long ROOT_PID = 0L;

    @Override
    public List<EquipmentTypeTreeVO> queryTreeList() {
        List<EquipmentType> all = this.list(
                new LambdaQueryWrapper<EquipmentType>()
                        .orderByAsc(EquipmentType::getSort));
        Map<Long, List<EquipmentTypeTreeVO>> childrenMap = all.stream()
                .map(this::toTreeVO)
                .collect(Collectors.groupingBy(EquipmentTypeTreeVO::getTypePid));
        return buildTree(ROOT_PID, childrenMap);
    }

    @Override
    public EquipmentType getById(Long typeId) {
        if (typeId == null) {
            throw new IllegalArgumentException("类型ID不能为空");
        }
        EquipmentType type = super.getById(typeId);
        if (type == null) {
            throw new IllegalArgumentException("设备类型不存在");
        }
        return type;
    }

    @Override
    public void saveType(EquipmentType equipmentType) {
        validate(equipmentType);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = currentUserId();
        if (equipmentType.getTypeId() == null) {
            equipmentType.setCreateTime(now);
            equipmentType.setCreateUserId(currentUserId);
            this.save(equipmentType);
        } else {
            equipmentType.setUpdateTime(now);
            equipmentType.setUpdateUserId(currentUserId);
            this.updateById(equipmentType);
        }
    }

    @Override
    public void delete(Long typeId) {
        if (typeId == null) {
            throw new IllegalArgumentException("类型ID不能为空");
        }
        if (super.getById(typeId) == null) {
            throw new IllegalArgumentException("设备类型不存在");
        }
        Long childrenCount = this.count(
                new LambdaQueryWrapper<EquipmentType>()
                        .eq(EquipmentType::getTypePid, typeId));
        if (childrenCount > 0) {
            throw new IllegalArgumentException("该类型下存在下级类型，无法删除");
        }
        super.removeById(typeId);
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
     * @param equipmentType 类型信息
     */
    private void validate(EquipmentType equipmentType) {
        if (equipmentType == null) {
            throw new IllegalArgumentException("设备类型信息不能为空");
        }
        if (!StringUtils.hasText(equipmentType.getTypeName())) {
            throw new IllegalArgumentException("类型名称不能为空");
        }
        if (!StringUtils.hasText(equipmentType.getTypeCode())) {
            throw new IllegalArgumentException("类型编号不能为空");
        }
        if (equipmentType.getTypePid() == null) {
            equipmentType.setTypePid(ROOT_PID);
        }
        if (equipmentType.getSort() == null) {
            equipmentType.setSort(0);
        }
    }

    /**
     * 递归构建类型树，仅保留从根节点可达的节点
     *
     * @param pid         父级ID
     * @param childrenMap 父级ID -> 子节点列表
     * @return 类型树列表
     */
    private List<EquipmentTypeTreeVO> buildTree(Long pid,
                                                Map<Long, List<EquipmentTypeTreeVO>> childrenMap) {
        List<EquipmentTypeTreeVO> children = childrenMap.get(pid);
        if (children == null || children.isEmpty()) {
            return new ArrayList<>();
        }
        children.forEach(child -> child.setChildren(buildTree(child.getTypeId(), childrenMap)));
        return children;
    }

    /**
     * 实体转为树形 VO
     *
     * @param type 类型实体
     * @return 树形 VO
     */
    private EquipmentTypeTreeVO toTreeVO(EquipmentType type) {
        EquipmentTypeTreeVO vo = new EquipmentTypeTreeVO();
        vo.setTypeId(type.getTypeId());
        vo.setTypeCode(type.getTypeCode());
        vo.setTypeName(type.getTypeName());
        vo.setTypePid(type.getTypePid());
        vo.setSort(type.getSort());
        vo.setCreateTime(type.getCreateTime());
        vo.setCreateUserId(type.getCreateUserId());
        vo.setUpdateTime(type.getUpdateTime());
        vo.setUpdateUserId(type.getUpdateUserId());
        return vo;
    }
}
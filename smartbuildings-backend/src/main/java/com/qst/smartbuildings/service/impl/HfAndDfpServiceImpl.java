package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.HfDfpAlarmInfoVO;
import com.qst.smartbuildings.entity.MonitorValue;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.HfAndDfpMapper;
import com.qst.smartbuildings.service.HfAndDfpService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.qst.smartbuildings.common.EquipmentTypeConst.Deep_Foundation_PIT_TYPE_ID;
import static com.qst.smartbuildings.common.EquipmentTypeConst.HIGH_FORMWORK_TYPE_ID;

/**
 * 高支模/深基坑业务服务实现
 *
 * @author 智能建造团队
 */
@Service
public class HfAndDfpServiceImpl implements HfAndDfpService {

    /** 高支模/深基坑（基坑监测）报警类型 */
    private static final String MONITOR_ALERT_TYPE = "1";
    /** 预警 */
    private static final String ALERT_TYPE_WARNING = "0";
    /** 报警 */
    private static final String ALERT_TYPE_ALARM = "1";
    /** 每个监测点最多返回的数据条数 */
    private static final int MAX_RECORDS_PER_POINT = 10;

    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final HfAndDfpMapper hfAndDfpMapper;

    public HfAndDfpServiceImpl(EquipmentAssetsMapper equipmentAssetsMapper,
                               HfAndDfpMapper hfAndDfpMapper) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.hfAndDfpMapper = hfAndDfpMapper;
    }

    @Override
    public List<EquipmentAssets> hfList() {
        return equipmentAssetsMapper.selectList(new LambdaQueryWrapper<EquipmentAssets>()
                .eq(EquipmentAssets::getTypeId, HIGH_FORMWORK_TYPE_ID)
                .orderByAsc(EquipmentAssets::getSort));
    }

    @Override
    public List<EquipmentAssets> dfpList() {
        return equipmentAssetsMapper.selectList(new LambdaQueryWrapper<EquipmentAssets>()
                .eq(EquipmentAssets::getTypeId, Deep_Foundation_PIT_TYPE_ID)
                .orderByAsc(EquipmentAssets::getSort));
    }

    @Override
    public Map<String, List<MonitorValue>> getMonitorData(Long assetId) {
        validateAssetId(assetId);
        Map<String, List<MonitorValue>> grouped = new LinkedHashMap<>();
        for (MonitorValue value : hfAndDfpMapper.selectMonitorValueList(assetId)) {
            List<MonitorValue> list = grouped.computeIfAbsent(value.getMonitorPointName(), k -> new ArrayList<>());
            if (list.size() < MAX_RECORDS_PER_POINT) {
                list.add(value);
            }
        }
        return grouped;
    }

    @Override
    public HfDfpAlarmInfoVO getAlarmInfo(Long assetId) {
        validateAssetId(assetId);
        HfDfpAlarmInfoVO vo = new HfDfpAlarmInfoVO();
        vo.setAlarmTypeList(hfAndDfpMapper.selectAlarmTypeList(assetId, MONITOR_ALERT_TYPE));
        vo.setAlarmCountList(hfAndDfpMapper.selectAlarmDays(assetId, MONITOR_ALERT_TYPE, ALERT_TYPE_ALARM));
        vo.setWarnCountList(hfAndDfpMapper.selectAlarmDays(assetId, MONITOR_ALERT_TYPE, ALERT_TYPE_WARNING));
        return vo;
    }

    /**
     * 校验设备ID
     */
    private void validateAssetId(Long assetId) {
        if (assetId == null || Objects.equals(assetId, 0L)) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
    }
}

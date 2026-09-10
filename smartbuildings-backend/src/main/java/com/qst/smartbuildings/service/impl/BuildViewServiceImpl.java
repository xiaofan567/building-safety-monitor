package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smartbuildings.dto.CountTrendDTO;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.mapper.BuildViewMapper;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.service.BuildViewService;
import com.qst.smartbuildings.vo.EquipmentOverviewVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 机械设备总览业务服务实现
 *
 * @author 智能建造团队
 */
@Service
public class BuildViewServiceImpl implements BuildViewService {

    /** 报警分类：报警 */
    private static final String ALERT_TYPE_ALARM = "1";
    /** 报警分类：预警 */
    private static final String ALERT_TYPE_WARNING = "0";
    /** 在线状态（offline_status）：在线 */
    private static final String ONLINE_STATUS = "1";

    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final BuildViewMapper buildViewMapper;

    public BuildViewServiceImpl(EquipmentAssetsMapper equipmentAssetsMapper,
                                BuildViewMapper buildViewMapper) {
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.buildViewMapper = buildViewMapper;
    }

    @Override
    public EquipmentOverviewVO overview(Long typeId) {
        validateTypeId(typeId);

        List<EquipmentAssets> assetList = equipmentAssetsMapper.selectList(
                new LambdaQueryWrapper<EquipmentAssets>()
                        .eq(EquipmentAssets::getTypeId, typeId)
                        .orderByAsc(EquipmentAssets::getSort));

        // 设备总数 / 在线数
        long assetCount = assetList.size();
        long onlineCount = assetList.stream()
                .filter(asset -> ONLINE_STATUS.equals(asset.getOfflineStatus()))
                .count();

        // 昨日/对比日时间范围
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime compareStart = today.minusDays(2).atStartOfDay();

        EquipmentOverviewVO vo = new EquipmentOverviewVO();
        vo.setEquipmentAssetsCount((int) assetCount);
        vo.setEquipmentAssetsOnlineCount((int) onlineCount);
        vo.setEquipmentAssetsList(assetList);
        vo.setAlarmCount(buildCountTrend(typeId, ALERT_TYPE_ALARM,
                compareStart, yesterdayStart, todayStart));
        vo.setWarnCount(buildCountTrend(typeId, ALERT_TYPE_WARNING,
                compareStart, yesterdayStart, todayStart));
        return vo;
    }

    /**
     * 构建报警/预警数量趋势：昨日数量、对比日数量、环比增长率
     */
    private CountTrendDTO buildCountTrend(Long typeId, String alertType,
                                          LocalDateTime compareStart,
                                          LocalDateTime yesterdayStart,
                                          LocalDateTime todayStart) {
        Integer compareDayCount = nvl(buildViewMapper.selectAlertCountByTypeAndRange(
                typeId, alertType, compareStart, yesterdayStart));
        Integer yesterdayCount = nvl(buildViewMapper.selectAlertCountByTypeAndRange(
                typeId, alertType, yesterdayStart, todayStart));

        CountTrendDTO trend = new CountTrendDTO();
        trend.setCompareDayCount(compareDayCount);
        trend.setYesterdayCount(yesterdayCount);
        trend.setGrowthRate(calcGrowthRate(yesterdayCount, compareDayCount));
        return trend;
    }

    /**
     * 计算环比增长率（百分比）。对比日为0时，昨日>0视为100%，否则为0%。
     */
    private int calcGrowthRate(int yesterdayCount, int compareDayCount) {
        if (compareDayCount == 0) {
            return yesterdayCount > 0 ? 100 : 0;
        }
        return (int) Math.round((yesterdayCount - compareDayCount) * 100.0 / compareDayCount);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateTypeId(Long typeId) {
        if (typeId == null || Objects.equals(typeId, 0L)) {
            throw new IllegalArgumentException("设备类型ID不能为空");
        }
    }
}
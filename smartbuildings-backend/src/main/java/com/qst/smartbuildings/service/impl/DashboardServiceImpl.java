package com.qst.smartbuildings.service.impl;

import com.qst.smartbuildings.mapper.DashboardMapper;
import com.qst.smartbuildings.mapper.EnvironmentValueMapper;
import com.qst.smartbuildings.service.DashboardService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页数据看板业务服务实现
 *
 * @author 智能建造团队
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 首页各模块默认返回的条数 */
    private static final int DEFAULT_LIMIT = 10;

    private final DashboardMapper dashboardMapper;
    private final EnvironmentValueMapper environmentValueMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper,
                                EnvironmentValueMapper environmentValueMapper) {
        this.dashboardMapper = dashboardMapper;
        this.environmentValueMapper = environmentValueMapper;
    }

    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 最近喷淋记录
        data.put("spraySystemList", dashboardMapper.selectSprayList(DEFAULT_LIMIT));
        // 环境实时数据（每个监测点最新一条）
        data.put("realTimeList", environmentValueMapper.getRealTimeList());
        // 危大工程设备统计（塔吊、升降机、高支模、深基坑）
        data.put("buildAssetCount", dashboardMapper.selectBuildAssetCount());

        // 最近报警记录（首页列表与分页列表展示同一组数据）
        Object recentAlertList = dashboardMapper.selectRecentAlertList(DEFAULT_LIMIT);
        data.put("alertList", recentAlertList);
        data.put("alertListPage", recentAlertList);

        // 报警类型聚合统计
        data.put("alertTypeAndCount", dashboardMapper.selectAlertTypeCount());
        return data;
    }
}
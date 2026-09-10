package com.qst.smartbuildings.service;

import java.util.Map;

/**
 * 首页数据看板业务服务
 *
 * @author 智能建造团队
 */
public interface DashboardService {

    /**
     * 查询首页数据看板数据
     *
     * @return 首页看板数据（喷淋记录、环境实时数据、危大工程设备统计、报警记录、报警类型统计等）
     */
    Map<String, Object> getDashboardData();
}
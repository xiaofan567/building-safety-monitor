package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorPointAlertParam;

/**
 * 监测点警报服务
 *
 * @author 智能建造团队
 */
public interface MonitorPointAlertService extends IService<MonitorPointAlert> {

    /**
     * 多条件分页查询报警记录
     *
     * @param monitorPointAlert 查询条件（分页、报警分类、设备、处理状态、时间范围等）
     * @return 报警记录分页数据
     */
    BPage<MonitorPointAlert> selectPage(MonitorPointAlertParam monitorPointAlert);
}

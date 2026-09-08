package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorPointAlertParam;
import com.qst.smartbuildings.mapper.MonitorPointAlertMapper;
import com.qst.smartbuildings.service.MonitorPointAlertService;
import org.springframework.stereotype.Service;

/**
 * 监测点警报服务实现
 *
 * @author 智能建造团队
 */
@Service
public class MonitorPointAlertServiceImpl
        extends ServiceImpl<MonitorPointAlertMapper, MonitorPointAlert>
        implements MonitorPointAlertService {

    @Override
    public BPage<MonitorPointAlert> selectPage(MonitorPointAlertParam monitorPointAlert) {
        return getBaseMapper().selectPointByPage(monitorPointAlert.parse(), monitorPointAlert);
    }
}

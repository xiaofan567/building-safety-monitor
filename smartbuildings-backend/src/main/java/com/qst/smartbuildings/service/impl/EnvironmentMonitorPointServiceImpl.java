package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.environment.EnvironmentMonitorPoint;
import com.qst.smartbuildings.mapper.EnvironmentMonitorPointMapper;
import com.qst.smartbuildings.service.EnvironmentMonitorPointService;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentMonitorPointServiceImpl extends ServiceImpl<EnvironmentMonitorPointMapper, EnvironmentMonitorPoint> implements EnvironmentMonitorPointService {
}

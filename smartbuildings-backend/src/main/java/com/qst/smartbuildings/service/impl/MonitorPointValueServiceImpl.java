package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.MonitorValue;
import com.qst.smartbuildings.mapper.MonitorPointValueMapper;
import com.qst.smartbuildings.service.MonitorPointValueService;
import org.springframework.stereotype.Service;

/**
 * 监测点监测数据服务实现
 *
 * @author 智能建造团队
 */
@Service
public class MonitorPointValueServiceImpl
        extends ServiceImpl<MonitorPointValueMapper, MonitorValue>
        implements MonitorPointValueService {
}

package com.qst.smartbuildings.service.environment.impl;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.SprayTask;
import com.qst.smartbuildings.entity.param.SprayTaskParam;
import com.qst.smartbuildings.mapper.environment.SprayTaskMapper;
import com.qst.smartbuildings.service.environment.SprayTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 喷淋任务服务实现类
 *
 * @author system
 * @since 1.0
 */
@Service
public class SprayTaskServiceImpl extends ServiceImpl<SprayTaskMapper, SprayTask> implements SprayTaskService {

    @Override
    public BPage<SprayTask> selectTaskByPage(SprayTaskParam sprayTaskParam) {
        return getBaseMapper().selectTaskByPage(sprayTaskParam.parse(), sprayTaskParam);
    }
}

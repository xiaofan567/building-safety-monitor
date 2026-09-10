package com.qst.smartbuildings.service.environment.impl;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.param.SpraySystemParam;
import com.qst.smartbuildings.mapper.environment.SpraySystemMapper;
import com.qst.smartbuildings.service.environment.SpraySystemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 喷淋系统服务实现类
 *
 * @author system
 * @since 1.0
 */
@Service
public class SpraySystemServiceImpl extends ServiceImpl<SpraySystemMapper, SpraySystem> implements SpraySystemService {

    @Override
    public BPage<SpraySystem> selectByPage(SpraySystemParam spraySystemParam) {
        return getBaseMapper().selectByPage(spraySystemParam.parse(), spraySystemParam);
    }
}

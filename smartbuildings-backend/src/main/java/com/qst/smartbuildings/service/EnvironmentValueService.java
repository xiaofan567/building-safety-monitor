package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.EnvironmentValue;
import com.qst.smartbuildings.entity.environment.param.EnvironmentValueParam;

import java.util.Map;

public interface EnvironmentValueService extends IService<EnvironmentValue> {
    Map<String, Object> getEnvironmentInfo();

    BPage<EnvironmentValue> selectByPage(EnvironmentValueParam environmentValueParam);
}
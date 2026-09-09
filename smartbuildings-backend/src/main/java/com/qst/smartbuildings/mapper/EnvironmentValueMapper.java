package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.EnvironmentValue;
import com.qst.smartbuildings.entity.environment.param.EnvironmentValueParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface EnvironmentValueMapper extends BaseMapper<EnvironmentValue> {
    List<EnvironmentValue> getRealTimeList();
    //统计一周内空气质量
    List<Map<String, Integer>> countAirQuality();
    //
    BPage<EnvironmentValue> selectByPage(BPage<EnvironmentValue> parse, @Param("data") EnvironmentValueParam environmentValueParam);
}

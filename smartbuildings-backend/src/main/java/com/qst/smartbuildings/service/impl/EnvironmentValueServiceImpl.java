package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.EnvironmentValue;
import com.qst.smartbuildings.entity.environment.param.EnvironmentValueParam;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.param.SpraySystemParam;
import com.qst.smartbuildings.mapper.EnvironmentValueMapper;
import com.qst.smartbuildings.mapper.environment.SpraySystemMapper;
import com.qst.smartbuildings.service.EnvironmentValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnvironmentValueServiceImpl extends ServiceImpl<EnvironmentValueMapper, EnvironmentValue> implements EnvironmentValueService {
    @Autowired
    private SpraySystemMapper spraySystemMapper;

    // a)实时温湿度，风速，噪声，TSP，PM10，PM2.5
    //     b)24小时内的空气质量（pm10，pm2.5），噪声，
    //     c)近一周温度（最高温度，最低温度）
    //     d)近一周的空气质量平均值（pm2.5）
    //     e)历史空气质量分析0<优<35<良<75<轻度污染<115<重度污染<150<重度污染<250<严重污染
    //     f)喷淋启停记录
    @Override
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> result = new HashMap<>();
        //查询每个类型下的最新环境数据
        List<EnvironmentValue> realTimeList = getBaseMapper().getRealTimeList();
        //获取一天前的时间
        //        Date oneDayAgo = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        //        写死时间，后续再调整
        Date oneDayAgo = new Date(1753708800000L - 24 * 60 * 60 * 1000);

        //查询24小时的pm2.5质量
        List<EnvironmentValue> pm25List = baseMapper.selectList(
                new LambdaQueryWrapper<EnvironmentValue>()
                        .eq(EnvironmentValue::getMonitorPointCode, "pm25")
                        .eq(EnvironmentValue::getDateType, "hour_time")
                        .gt(EnvironmentValue::getMonitorTime, oneDayAgo));
        //查询24小时的pm10质量
        List<EnvironmentValue> pm10List = baseMapper.selectList(
                new LambdaQueryWrapper<EnvironmentValue>()
                        .eq(EnvironmentValue::getMonitorPointCode, "pm10")
                        .eq(EnvironmentValue::getDateType, "hour_time")
                        .gt(EnvironmentValue::getMonitorTime, oneDayAgo));
        //查询24小时内的噪声
        List<EnvironmentValue> noiseList = baseMapper.selectList(
                new LambdaQueryWrapper<EnvironmentValue>()
                        .eq(EnvironmentValue::getMonitorPointCode, "noise")
                        .eq(EnvironmentValue::getDateType, "hour_time")
                        .gt(EnvironmentValue::getMonitorTime, oneDayAgo));
        //获取一周前的时间
        //        Date oneWeekAgo = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
        Date oneWeekAgo = new Date(1753708800000L - 7 * 24 * 60 * 60 * 1000);

        //查询近一周的温度
        List<EnvironmentValue> temperatureList = baseMapper.selectList(
                new LambdaQueryWrapper<EnvironmentValue>()
                        .eq(EnvironmentValue::getMonitorPointCode, "temp")
                        .eq(EnvironmentValue::getDateType, "day_time")
                        .gt(EnvironmentValue::getMonitorTime, oneWeekAgo));
        //查询近一周的空气质量平均值（pm2.5）
        List<EnvironmentValue> pm25AvgList = baseMapper.selectList(
                new LambdaQueryWrapper<EnvironmentValue>()
                        .eq(EnvironmentValue::getMonitorPointCode, "pm25")
                        .eq(EnvironmentValue::getDateType, "day_time")
                        .gt(EnvironmentValue::getMonitorTime, oneWeekAgo));
        //查询一周内的空气质量
        List<Map<String, Integer>> airQuality = baseMapper.countAirQuality();

        //查询7条喷淋记录
        BPage<SpraySystem> spraySystemPage = spraySystemMapper.selectByPage(new BPage<>(1, 7), new SpraySystemParam());

        //将查询到的数据放入result中
        result.put("realTimeList", realTimeList);
        result.put("pm25List", pm25List);
        result.put("pm10List", pm10List);
        result.put("noiseList", noiseList);
        result.put("temperatureList", temperatureList);
        result.put("pm25AvgList", pm25AvgList);
        result.put("airQuality", airQuality);
        result.put("spraySystem", spraySystemPage.getRecords());
        return result;
    }

    @Override
    public BPage<EnvironmentValue> selectByPage(EnvironmentValueParam environmentValueParam) {
        return getBaseMapper().selectByPage(environmentValueParam.parse(), environmentValueParam);
    }
}
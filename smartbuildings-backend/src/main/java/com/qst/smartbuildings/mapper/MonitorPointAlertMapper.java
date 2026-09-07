package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.MonitorPointAlertParam;
import org.apache.ibatis.annotations.Param;

/**
 * 监测点警报 Mapper
 *
 * @author 智能建造团队
 */
public interface MonitorPointAlertMapper extends BaseMapper<MonitorPointAlert> {

    /**
     * 多条件分页查询报警记录（联表补充设备、监测点、位置信息）
     *
     * @param parse 分页参数
     * @param param 查询条件
     * @return 报警记录分页数据
     */
    BPage<MonitorPointAlert> selectPointByPage(BPage<Object> parse, @Param("data") MonitorPointAlertParam param);
}

package com.qst.smartbuildings.mapper;

import com.qst.smartbuildings.entity.AlertTypeCountItem;
import com.qst.smartbuildings.entity.MonitorPointAlert;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.vo.BuildAssetCountVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 首页数据看板 Mapper
 *
 * <p>聚合查询危大工程设备统计、喷淋记录、报警记录及报警类型统计等看板数据。</p>
 *
 * @author 智能建造团队
 */
public interface DashboardMapper {

    /**
     * 统计危大工程各设备类型（塔吊、升降机、高支模、深基坑）的设备总数、在线数及报警数
     */
    List<BuildAssetCountVO> selectBuildAssetCount();

    /**
     * 按报警内容分组统计报警数量（降序）
     */
    List<AlertTypeCountItem> selectAlertTypeCount();

    /**
     * 查询最近喷淋记录（联表补充监测点、设备、位置信息）
     *
     * @param limit 返回条数
     */
    List<SpraySystem> selectSprayList(@Param("limit") int limit);

    /**
     * 查询最近报警记录（联表补充设备、监测点、位置信息）
     *
     * @param limit 返回条数
     */
    List<MonitorPointAlert> selectRecentAlertList(@Param("limit") int limit);
}
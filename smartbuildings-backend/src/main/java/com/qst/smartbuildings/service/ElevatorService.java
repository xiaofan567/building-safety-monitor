package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.ElevatorAlarmInfoVO;
import com.qst.smartbuildings.entity.ElevatorData;
import com.qst.smartbuildings.entity.ElevatorInfo;
import com.qst.smartbuildings.entity.ElevatorWorkInfo;

import java.util.List;

/**
 * 升降机（施工升降电梯）业务服务
 *
 * @author 智能建造团队
 */
public interface ElevatorService {

    /**
     * 查询升降机列表
     */
    List<ElevatorInfo> selectAllElevator();

    /**
     * 分页查询升降机监测历史记录
     */
    BPage<ElevatorData> elevatorDataList(Long assetId, long page, long limit);

    /**
     * 分页查询工作循环记录
     */
    BPage<ElevatorWorkInfo> elevatorWorkList(Long assetId, long page, long limit);

    /**
     * 获取升降机实时数据（最近10条）
     */
    List<ElevatorData> getElevatorData(Long assetId);

    /**
     * 获取升降机报警信息聚合
     */
    ElevatorAlarmInfoVO getElevatorAlarmInfo(Long assetId);
}
package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.TowerAlarmInfoVO;
import com.qst.smartbuildings.entity.TowerData;
import com.qst.smartbuildings.entity.TowerInfo;
import com.qst.smartbuildings.entity.TowerWorkInfo;
import com.qst.smartbuildings.entity.TowerWorkInfoVO;

import java.util.List;

/**
 * 塔吊（塔机）业务服务
 *
 * @author 智能建造团队
 */
public interface TowerService {

    /**
     * 查询塔吊列表
     */
    List<TowerInfo> selectAllTower();

    /**
     * 分页查询塔机监测历史记录
     */
    BPage<TowerData> towerDataList(Long assetId, long page, long limit);

    /**
     * 分页查询工作循环记录
     */
    BPage<TowerWorkInfo> towerWorkList(Long assetId, long page, long limit);

    /**
     * 查询塔吊实时数据（最近10条）
     */
    List<TowerData> getTowerData(Long assetId);

    /**
     * 查询塔吊今日工作状态
     */
    TowerWorkInfoVO getTowerWorkInfo(Long assetId);

    /**
     * 查询塔吊报警记录聚合
     */
    TowerAlarmInfoVO getTowerAlarmInfo(Long assetId);
}
package com.qst.smartbuildings.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 机械设备总览 Mapper
 *
 * <p>用于统计危大工程设备（塔吊、升降机、高支模、深基坑）按设备类型+时间范围的报警/预警数量。</p>
 *
 * @author 智能建造团队
 */
public interface BuildViewMapper {

    /**
     * 统计指定设备类型、指定报警分类、指定时间范围内的报警/预警数量
     *
     * @param typeId    设备类型ID（如：145塔吊、115升降机、147高支模、148深基坑）
     * @param alertType 报警分类 0预警 1报警
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @return 数量
     */
    Integer selectAlertCountByTypeAndRange(@Param("typeId") Long typeId,
                                           @Param("alertType") String alertType,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}
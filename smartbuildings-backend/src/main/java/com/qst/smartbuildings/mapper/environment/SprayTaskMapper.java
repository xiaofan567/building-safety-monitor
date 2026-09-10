package com.qst.smartbuildings.mapper.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.SprayTask;
import com.qst.smartbuildings.entity.param.SprayTaskParam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 喷淋任务数据操作接口
 *
 * @author system
 * @since 1.0
 */
@Mapper
public interface SprayTaskMapper extends BaseMapper<SprayTask> {

    /**
     * 分页查询喷淋任务
     *
     * @param page 分页参数
     * @param sprayTaskParam 查询条件
     * @return 分页结果
     */
    BPage<SprayTask> selectTaskByPage(BPage<SprayTask> page, @Param("data") SprayTaskParam sprayTaskParam);
}
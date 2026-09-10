package com.qst.smartbuildings.mapper.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.param.SpraySystemParam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 喷淋系统数据操作接口
 *
 * @author system
 * @since 1.0
 */
@Mapper
public interface SpraySystemMapper extends BaseMapper<SpraySystem> {

    /**
     * 分页查询喷淋系统记录
     *
     * @param page 分页参数
     * @param spraySystemParam 查询条件
     * @return 分页结果
     */
    BPage<SpraySystem> selectByPage(BPage<SpraySystem> page, @Param("data") SpraySystemParam spraySystemParam);
}
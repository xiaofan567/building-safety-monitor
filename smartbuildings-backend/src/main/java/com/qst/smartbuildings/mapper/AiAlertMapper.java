package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.entity.camera.param.AiAlertParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAlertMapper extends BaseMapper<AiAlert> {
    BPage<AiAlert> selectPage(BPage<AiAlert> parse, @Param("data") AiAlertParam aiAlertParam);

    AiAlert getAiAlertById(@Param("id") Integer id);
}

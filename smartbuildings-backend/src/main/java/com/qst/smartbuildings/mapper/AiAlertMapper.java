package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.entity.camera.param.AiAlertParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiAlertMapper extends BaseMapper<AiAlert> {
    BPage<AiAlert> selectPage(BPage<AiAlert> parse, @Param("data") AiAlertParam aiAlertParam);

    AiAlert getAiAlertById(@Param("id") Integer id);

    /**
     * 统计 AI 报警按报警类型（alert_type）的累计数量，供日报 AI 报警章节使用。
     *
     * @return 每行包含 alertType / numb
     */
    List<Map<String, Object>> getAlertTypeAndCount();
}

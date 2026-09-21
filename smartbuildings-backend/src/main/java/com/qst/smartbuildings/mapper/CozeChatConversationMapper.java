package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.coze.CozeChatConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Coze 会话数据操作接口。
 */
@Mapper
public interface CozeChatConversationMapper extends BaseMapper<CozeChatConversation> {
}
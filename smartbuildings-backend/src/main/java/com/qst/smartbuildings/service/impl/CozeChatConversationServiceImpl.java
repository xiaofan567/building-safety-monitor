package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.coze.CozeChatConversation;
import com.qst.smartbuildings.mapper.CozeChatConversationMapper;
import com.qst.smartbuildings.service.CozeChatConversationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coze 本地会话记录服务实现。
 */
@Service
public class CozeChatConversationServiceImpl
        extends ServiceImpl<CozeChatConversationMapper, CozeChatConversation>
        implements CozeChatConversationService {

    @Override
    public List<CozeChatConversation> listByUserId(Long userId) {
        LambdaQueryWrapper<CozeChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CozeChatConversation::getUserId, userId)
                .orderByDesc(CozeChatConversation::getUpdatedAt)
                .orderByDesc(CozeChatConversation::getId);
        return list(wrapper);
    }

    @Override
    public CozeChatConversation saveConversation(Long userId, String conversationId, String title) {
        CozeChatConversation conversation = new CozeChatConversation()
                .setUserId(userId)
                .setConversationId(conversationId)
                .setTitle(title != null && !title.isBlank() ? title : "新对话");
        save(conversation);
        return conversation;
    }

    @Override
    public boolean deleteById(Long id) {
        return removeById(id);
    }
}
package com.qst.smartbuildings.service;

import com.qst.smartbuildings.entity.coze.CozeChatConversation;

import java.util.List;

/**
 * Coze 本地会话记录服务。
 */
public interface CozeChatConversationService {

    List<CozeChatConversation> listByUserId(Long userId);

    CozeChatConversation saveConversation(Long userId, String conversationId, String title);

    boolean deleteById(Long id);
}
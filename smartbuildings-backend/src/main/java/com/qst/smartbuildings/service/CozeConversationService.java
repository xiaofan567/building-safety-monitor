package com.qst.smartbuildings.service;

import java.util.Map;

/**
 * Coze 远程会话 API。
 */
public interface CozeConversationService {

    Map<String, Object> createConversation(String name);

    Map<String, Object> retrieveConversation(String conversationId);
}
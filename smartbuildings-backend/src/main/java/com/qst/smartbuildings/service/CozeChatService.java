package com.qst.smartbuildings.service;

/**
 * Coze 对话 API。
 */
public interface CozeChatService {

    String callCozeApi(String dynamicContent);

    String callCozeApiWithConversation(String dynamicContent, String conversationId);

    String extractMessageContent(String responseRaw);
}
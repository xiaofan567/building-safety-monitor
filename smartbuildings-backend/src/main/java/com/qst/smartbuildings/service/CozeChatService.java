package com.qst.smartbuildings.service;

/**
 * Coze 对话 API。
 */
public interface CozeChatService {

    String callCozeApi(String dynamicContent);

    String callCozeApiWithConversation(String dynamicContent, String conversationId);

    String extractMessageContent(String responseRaw);

    /**
     * 完整的 Coze 定时任务执行流程：
     * 1. 获取安全日报数据
     * 2. 创建调用记录（status=0）
     * 3. 调用 Coze API
     * 4. 解析响应，更新记录状态（成功 status=1，失败 status=2）
     *
     * @return true 表示执行成功，false 表示失败
     */
    boolean executeAndRecord();
}
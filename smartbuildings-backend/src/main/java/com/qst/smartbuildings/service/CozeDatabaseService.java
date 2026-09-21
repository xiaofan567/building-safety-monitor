package com.qst.smartbuildings.service;

/**
 * Coze 知识库（Database）相关业务接口。
 *
 * <p>用于管理日报模板与报警规则提示词数据，提供按 prompt_key 的查询与更新能力。</p>
 */
public interface CozeDatabaseService {

    /**
     * 根据 prompt_key 查询 prompt_text 内容。
     *
     * @param promptKey 提示词唯一标识（如 "doc_template"、"rule_template"）
     * @return 对应的 prompt_text 字符串，无数据时返回 null
     */
    String queryRecord(String promptKey);

    /**
     * 更新日报模板（prompt_key = "doc_template"）内容。
     *
     * @param promptText 模板的 Markdown 内容
     * @return Coze API 返回的完整响应字符串
     */
    String updateTemplate(String promptText);

    /**
     * 更新报警规则（prompt_key = "rule_template"）内容。
     *
     * @param promptText 报警配置的 Markdown 内容
     * @return Coze API 返回的完整响应字符串
     */
    String updateAlertConfig(String promptText);

    /**
     * 同步 Coze 知识库文档（占位实现，子任务继续完善）。
     *
     * @return 同步结果描述
     */
    String syncDocuments();
}

package com.qst.smartbuildings.service;

/**
 * Coze 知识库（Database）相关业务接口。
 *
 * <p>用于管理日报模板与报警规则提示词数据，以及定时同步 Coze 平台生成的报告文档。</p>
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
     * 同步 Coze 知识库（第二个数据库 safe_report_list）中的报告文档到本地。
     *
     * <p>流程：
     * <ol>
     *     <li>查询 Coze 知识库获取所有文档记录（doc_id, doc_name, bstudio_create_time, doc_url）；</li>
     *     <li>对比本地数据库已有的 doc_id，只处理新增的文档；</li>
     *     <li>下载 doc_url 文件到本地存储目录；</li>
     *     <li>将记录写入本地数据库，doc_url 替换为本地相对路径。</li>
     * </ol>
     *
     * @return 同步结果描述（含 总数 / 新增 / 跳过 / 失败 统计）
     */
    String syncDocuments();
}
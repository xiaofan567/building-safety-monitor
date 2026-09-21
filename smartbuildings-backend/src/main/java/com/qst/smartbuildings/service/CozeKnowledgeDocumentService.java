package com.qst.smartbuildings.service;

import java.util.List;
import java.util.Map;

/**
 * Coze 知识库文档远程 API。
 */
public interface CozeKnowledgeDocumentService {

    /**
     * 查询知识库文档列表。
     *
     * @param datasetId 知识库 ID
     * @param page      页码
     * @param size      每页条数
     * @return 文档列表与总数
     */
    Map<String, Object> listDocuments(String datasetId, Integer page, Integer size);

    /**
     * 上传文档到知识库。
     *
     * @param datasetId   知识库 ID
     * @param name        文件名
     * @param fileBase64  文件 base64 编码
     * @param fileType    文件类型后缀
     * @param formatType  解析格式类型
     * @param chunkType   分段类型
     * @return 上传结果
     */
    Map<String, Object> createDocument(String datasetId, String name, String fileBase64,
                                       String fileType, Integer formatType, Integer chunkType);

    /**
     * 删除知识库文档。
     *
     * @param documentIds 文档 ID 列表
     * @return 删除结果
     */
    Map<String, Object> deleteDocuments(List<String> documentIds);
}
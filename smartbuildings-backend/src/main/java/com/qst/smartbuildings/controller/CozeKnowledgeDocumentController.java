package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.CozeKnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Coze 知识库文档管理接口。
 */
@RestController
@RequestMapping("/coze/knowledge")
@Tag(name = "Coze 知识库文档管理", description = "Coze 知识库文档列表、上传与删除接口")
public class CozeKnowledgeDocumentController {

    @Autowired
    private CozeKnowledgeDocumentService cozeKnowledgeDocumentService;

    @Value("${coze.knowledge.dataset-id}")
    private String datasetId;

    @PostMapping("/document/list")
    @Operation(summary = "查看知识库文档列表", description = "调用 Coze open_api/knowledge/document/list 接口查询知识库中的文档列表，支持分页")
    public Result<Map<String, Object>> listDocuments(@RequestBody Map<String, Object> params) {
        Integer page = params.get("page") instanceof Number
                ? ((Number) params.get("page")).intValue() : null;
        Integer size = params.get("size") instanceof Number
                ? ((Number) params.get("size")).intValue() : null;

        try {
            Map<String, Object> result = cozeKnowledgeDocumentService.listDocuments(datasetId, page, size);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(500, "知识库文档列表查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/document/create")
    @Operation(summary = "上传文件到���识库", description = "调用 Coze open_api/knowledge/document/create 接口上传文件到知识库，支持 base64 编码的文件内容")
    public Result<Map<String, Object>> createDocument(@RequestBody Map<String, Object> params) {
        String name = params.get("name") instanceof String
                ? (String) params.get("name") : null;
        if (name == null || name.isEmpty()) {
            return Result.error(400, "name 不能为空");
        }

        String fileBase64 = params.get("fileBase64") instanceof String
                ? (String) params.get("fileBase64") : null;
        if (fileBase64 == null || fileBase64.isEmpty()) {
            return Result.error(400, "fileBase64 不能为空");
        }

        String fileType = params.get("fileType") instanceof String
                ? (String) params.get("fileType") : null;
        if (fileType == null || fileType.isEmpty()) {
            return Result.error(400, "fileType 不能为空");
        }

        Integer formatType = params.get("formatType") instanceof Number
                ? ((Number) params.get("formatType")).intValue() : 0;
        Integer chunkType = params.get("chunkType") instanceof Number
                ? ((Number) params.get("chunkType")).intValue() : null;

        try {
            Map<String, Object> result = cozeKnowledgeDocumentService.createDocument(
                    datasetId, name, fileBase64, fileType, formatType, chunkType);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(500, "知识库文档上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/document/delete")
    @Operation(summary = "删除知识库文档", description = "调用 Coze open_api/knowledge/document/delete 接口批量删除知识库中的文档")
    public Result<Map<String, Object>> deleteDocuments(@RequestBody Map<String, Object> params) {
        Object docIdsObj = params.get("documentIds");
        if (!(docIdsObj instanceof List)) {
            return Result.error(400, "documentIds 不能为空且必须是数组");
        }

        @SuppressWarnings("unchecked")
        List<Object> rawList = (List<Object>) docIdsObj;
        List<String> documentIds = new ArrayList<>();
        for (Object item : rawList) {
            documentIds.add(String.valueOf(item));
        }
        if (documentIds.isEmpty()) {
            return Result.error(400, "documentIds 不能为空");
        }

        try {
            Map<String, Object> result = cozeKnowledgeDocumentService.deleteDocuments(documentIds);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(500, "知识库文档删除失败: " + e.getMessage());
        }
    }
}
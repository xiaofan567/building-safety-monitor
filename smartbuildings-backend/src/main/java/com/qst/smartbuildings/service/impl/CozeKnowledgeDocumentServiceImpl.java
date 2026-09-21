package com.qst.smartbuildings.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qst.smartbuildings.service.CozeKnowledgeDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coze 知识库文档 API 实现：列表查询、上传、删除。
 */
@Service
public class CozeKnowledgeDocumentServiceImpl implements CozeKnowledgeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(CozeKnowledgeDocumentServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coze.api.key}")
    private String apiKey;

    @Value("${coze.knowledge.document.list-url}")
    private String documentListUrl;

    @Value("${coze.knowledge.document.create-url}")
    private String documentCreateUrl;

    @Value("${coze.knowledge.document.delete-url}")
    private String documentDeleteUrl;

    /**
     * 构建请求头：JSON + Bearer Auth + Agw-Js-Conv。
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("Agw-Js-Conv", "str");
        return headers;
    }

    @Override
    public Map<String, Object> listDocuments(String datasetId, Integer page, Integer size) {
        String url = documentListUrl;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dataset_id", datasetId);
        if (page != null) {
            body.put("page", page);
        }
        if (size != null) {
            body.put("size", size);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 知识库文档列表查询请求: datasetId={}, page={}, size={}", datasetId, page, size);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            String responseBody = response.getBody();
            log.info("Coze 知识库文档列表查询响应: HTTP状态码={}", response.getStatusCodeValue());

            checkCozeBusinessError(responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                Map<String, Object> emptyResult = new LinkedHashMap<>();
                emptyResult.put("documents", Collections.emptyList());
                emptyResult.put("total", 0);
                return emptyResult;
            }

            JSONObject root = JSON.parseObject(responseBody);
            int total = root.getIntValue("total");
            JSONArray documentInfos = root.getJSONArray("document_infos");

            List<Map<String, Object>> documents = new ArrayList<>();
            if (documentInfos != null) {
                for (int i = 0; i < documentInfos.size(); i++) {
                    JSONObject doc = documentInfos.getJSONObject(i);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("documentId", doc.getString("document_id"));
                    item.put("name", doc.getString("name"));
                    item.put("type", doc.getString("type"));
                    item.put("size", doc.getLongValue("size"));
                    item.put("status", doc.getIntValue("status"));
                    item.put("charCount", doc.getIntValue("char_count"));
                    item.put("sliceCount", doc.getIntValue("slice_count"));
                    item.put("sourceType", doc.getIntValue("source_type"));
                    item.put("hitCount", doc.getIntValue("hit_count"));
                    item.put("formatType", doc.getIntValue("format_type"));
                    item.put("createTime", doc.getLongValue("create_time"));
                    item.put("updateTime", doc.getLongValue("update_time"));
                    item.put("webUrl", doc.getString("web_url"));
                    documents.add(item);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("documents", documents);
            result.put("total", total);
            return result;

        } catch (Exception e) {
            log.error("Coze 知识库文档列表查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 知识库文档列表查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> createDocument(String datasetId, String name, String fileBase64,
                                              String fileType, Integer formatType, Integer chunkType) {
        String url = documentCreateUrl;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dataset_id", datasetId);

        List<Map<String, Object>> documentBases = new ArrayList<>();
        Map<String, Object> docBase = new LinkedHashMap<>();
        docBase.put("name", name);

        Map<String, Object> sourceInfo = new LinkedHashMap<>();
        sourceInfo.put("file_base64", fileBase64);
        sourceInfo.put("file_type", fileType);
        sourceInfo.put("document_source", 0);
        docBase.put("source_info", sourceInfo);
        documentBases.add(docBase);
        body.put("document_bases", documentBases);

        Map<String, Object> chunkStrategy = new LinkedHashMap<>();
        chunkStrategy.put("chunk_type", chunkType != null ? chunkType : 0);
        body.put("chunk_strategy", chunkStrategy);

        body.put("format_type", formatType != null ? formatType : 0);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 知识库文档新增请求: datasetId={}, name={}, fileType={}, formatType={}",
                datasetId, name, fileType, formatType);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            String responseBody = response.getBody();
            log.info("Coze 知识库文档新增响应: HTTP状态码={}", response.getStatusCodeValue());

            checkCozeBusinessError(responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                Map<String, Object> emptyResult = new LinkedHashMap<>();
                emptyResult.put("documentInfos", Collections.emptyList());
                return emptyResult;
            }

            JSONObject root = JSON.parseObject(responseBody);
            JSONArray documentInfos = root.getJSONArray("document_infos");

            List<Map<String, Object>> documents = new ArrayList<>();
            if (documentInfos != null) {
                for (int i = 0; i < documentInfos.size(); i++) {
                    JSONObject doc = documentInfos.getJSONObject(i);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("documentId", doc.getString("document_id"));
                    item.put("name", doc.getString("name"));
                    item.put("type", doc.getString("type"));
                    item.put("size", doc.getLongValue("size"));
                    item.put("status", doc.getIntValue("status"));
                    item.put("formatType", doc.getIntValue("format_type"));
                    documents.add(item);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("documentInfos", documents);
            return result;

        } catch (Exception e) {
            log.error("Coze 知识库文档新增失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 知识库文档新增失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> deleteDocuments(List<String> documentIds) {
        String url = documentDeleteUrl;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("document_ids", documentIds);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 知识库文档删除请求: count={}, ids={}", documentIds.size(), documentIds);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            String responseBody = response.getBody();
            log.info("Coze 知识库文档删除响应: HTTP状态码={}", response.getStatusCodeValue());

            checkCozeBusinessError(responseBody);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deletedCount", documentIds.size());
            return result;

        } catch (Exception e) {
            log.error("Coze 知识库文档删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 知识库文档删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查 Coze API 响应中的业务状态码（非 0 即视为业务错误）。
     */
    private void checkCozeBusinessError(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(responseBody);
            if (root != null && root.containsKey("code")) {
                int code = root.getIntValue("code");
                if (code != 0) {
                    String msg = root.getString("msg");
                    String errMsg = "Coze Knowledge API 业务错误: code=" + code + ", msg=" + msg;
                    throw new RuntimeException(errMsg);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
            // 非 JSON 响应，跳过检查
        }
    }
}
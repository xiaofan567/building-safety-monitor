package com.qst.smartbuildings.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qst.smartbuildings.service.CozeDatabaseService;
import com.qst.smartbuildings.util.RedisClient;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coze 知识库（Database）服务实现。
 *
 * <p>负责调用 Coze Knowledge Database API（v1/databases），提供
 * 日报模板与报警规则提示词的查询与更新能力。</p>
 */
@Service
public class CozeDatabaseServiceImpl implements CozeDatabaseService {

    private static final Logger log = LoggerFactory.getLogger(CozeDatabaseServiceImpl.class);

    /** 日报模板 prompt_key */
    private static final String KEY_DOC_TEMPLATE = "doc_template";
    /** 报警规则 prompt_key */
    private static final String KEY_RULE_TEMPLATE = "rule_template";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coze.api.key}")
    private String apiKey;

    @Value("${coze.database.id}")
    private String databaseId;

    @Value("${coze.database.url}")
    private String databaseUrl;

    @Autowired
    private RedisClient redisClient;

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    @Override
    public String queryRecord(String promptKey) {
        String url = databaseUrl + "/" + databaseId + "/records/query";

        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> selectFields = new LinkedHashMap<>();
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add("prompt_text");
        selectFields.put("field_names", fieldNames);
        body.put("select_fields", selectFields);

        Map<String, Object> filter = new LinkedHashMap<>();
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("left", "prompt_key");
        condition.put("operation", "equal");
        condition.put("right", promptKey);
        conditions.add(condition);
        filter.put("conditions", conditions);
        body.put("filter", filter);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 知识库查询请求: url={}, promptKey={}", url, promptKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            log.info("Coze 知识库查询响应: HTTP状态码={}",
                    response.getStatusCode() != null ? response.getStatusCode().value() : null);

            checkCozeBusinessError(responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                return null;
            }
            JSONObject root = JSON.parseObject(responseBody);
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                return null;
            }
            JSONArray items = data.getJSONArray("items");
            if (items == null || items.isEmpty()) {
                return null;
            }
            JSONObject firstItem = items.getJSONObject(0);
            String promptText = firstItem.getString("prompt_text");
            log.info("Coze 知识库查询结果: promptKey={}, length={}",
                    promptKey, promptText != null ? promptText.length() : 0);
            return promptText;
        } catch (RuntimeException e) {
            log.error("Coze 知识库查询失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Coze 知识库查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 知识库查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateTemplate(String promptText) {
        return updateRecord(KEY_DOC_TEMPLATE, promptText, "日报模板");
    }

    @Override
    public String updateAlertConfig(String promptText) {
        return updateRecord(KEY_RULE_TEMPLATE, promptText, "报警配置");
    }

    @Override
    public String syncDocuments() {
        // 占位实现：文档同步逻辑在后续任务中实现
        log.info("Coze 知识库文档同步触发");
        return "sync scheduled";
    }

    /**
     * 通用更新方法：基于 prompt_key 过滤记录，更新 prompt_text 字段。
     */
    private String updateRecord(String promptKey, String promptText, String bizName) {
        String url = databaseUrl + "/" + databaseId + "/records";

        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> updateFields = new ArrayList<>();
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("field_name", "prompt_text");
        field.put("value", promptText);
        updateFields.add(field);
        body.put("update_fields", updateFields);

        Map<String, Object> filter = new LinkedHashMap<>();
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("left", "prompt_key");
        condition.put("operation", "equal");
        condition.put("right", promptKey);
        conditions.add(condition);
        filter.put("conditions", conditions);
        body.put("filter", filter);

        body.put("is_async", true);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 知识库更新 {} 请求: url={}, promptKey={}", bizName, url, promptKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, requestEntity, String.class);
            String responseBody = response.getBody();
            log.info("Coze 知识库更新 {} 响应: HTTP状态码={}", bizName,
                    response.getStatusCode() != null ? response.getStatusCode().value() : null);

            checkCozeBusinessError(responseBody);

            return responseBody;
        } catch (RuntimeException e) {
            log.error("Coze 知识库更新 {} 失败: {}", bizName, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Coze 知识库更新 {} 失败: {}", bizName, e.getMessage(), e);
            throw new RuntimeException("Coze 知识库更新" + bizName + "失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查 Coze API 响应体中的业务错误码（非 0 视为业务错误）。
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
            // 非 JSON 响应或不含 code 字段，跳过业务错误检查
        }
    }
}

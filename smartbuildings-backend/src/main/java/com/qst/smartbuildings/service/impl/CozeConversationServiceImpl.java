package com.qst.smartbuildings.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qst.smartbuildings.service.CozeConversationService;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coze 远程会话 API 实现。
 */
@Service
public class CozeConversationServiceImpl implements CozeConversationService {

    private static final Logger log = LoggerFactory.getLogger(CozeConversationServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coze.api.key}")
    private String apiKey;

    @Value("${coze.bot.id}")
    private String botId;

    @Value("${coze.conversation.url}")
    private String conversationUrl;

    @Override
    public Map<String, Object> createConversation(String name) {
        String url = conversationUrl + "/create";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", botId);
        body.put("name", name != null && !name.isBlank() ? name : UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());
        log.info("Coze 创建会话请求: botId={}", botId);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            String responseBody = response.getBody();
            checkCozeBusinessError(responseBody);

            JSONObject data = JSON.parseObject(responseBody).getJSONObject("data");
            Map<String, Object> result = new LinkedHashMap<>();
            if (data != null) {
                result.put("conversationId", data.getString("id"));
                result.put("name", data.getString("name"));
                result.put("createdAt", data.getLong("created_at"));
            }
            return result;
        } catch (Exception e) {
            log.error("Coze 创建会话失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 创建会话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> retrieveConversation(String conversationId) {
        String url = conversationUrl + "/retrieve?conversation_id=" + conversationId;
        HttpEntity<Void> requestEntity = new HttpEntity<>(buildHeaders());
        log.info("Coze 查看会话请求: conversationId={}", conversationId);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, String.class);
            String responseBody = response.getBody();
            checkCozeBusinessError(responseBody);

            JSONObject data = JSON.parseObject(responseBody).getJSONObject("data");
            Map<String, Object> result = new LinkedHashMap<>();
            if (data != null) {
                result.put("conversationId", data.getString("id"));
                result.put("name", data.getString("name"));
                result.put("status", data.getString("status"));
                result.put("createdAt", data.getLong("created_at"));
                result.put("lastSectionId", data.getString("last_section_id"));
            }
            return result;
        } catch (Exception e) {
            log.error("Coze 查看会话失败: {}", e.getMessage(), e);
            throw new RuntimeException("Coze 查看会话失败: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private void checkCozeBusinessError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(responseBody);
            if (root != null && root.containsKey("code") && root.getIntValue("code") != 0) {
                throw new RuntimeException("Coze Conversation API 业务错误: code="
                        + root.getIntValue("code") + ", msg=" + root.getString("msg"));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
            // 非 JSON 响应交由上层统一处理。
        }
    }
}
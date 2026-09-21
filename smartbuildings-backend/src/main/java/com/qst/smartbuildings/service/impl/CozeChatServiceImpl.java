package com.qst.smartbuildings.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qst.smartbuildings.service.CozeChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coze 对话 API 实现，支持流式 SSE 和非流式响应。
 */
@Service
public class CozeChatServiceImpl implements CozeChatService {

    private static final Logger log = LoggerFactory.getLogger(CozeChatServiceImpl.class);
    private static final String ANSWER = "answer";
    private static final String FOLLOW_UP = "follow_up";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coze.api.url}")
    private String apiUrl;

    @Value("${coze.api.key}")
    private String apiKey;

    @Value("${coze.bot.id}")
    private String botId;

    @Value("${coze.user.id}")
    private String userId;

    @Value("${coze.auto-save-history}")
    private boolean autoSaveHistory;

    @Value("${coze.stream}")
    private boolean stream;

    @Override
    public String callCozeApi(String dynamicContent) {
        return callCozeApiWithConversation(dynamicContent, null);
    }

    @Override
    public String callCozeApiWithConversation(String dynamicContent, String conversationId) {
        long start = System.currentTimeMillis();
        try {
            String result = stream
                    ? callStreamApi(start, dynamicContent, conversationId)
                    : callNonStreamApi(start, dynamicContent, conversationId);
            return result;
        } catch (RuntimeException e) {
            log.error("Coze API 调用异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Coze API 调用异常: conversationId={}, error={}", conversationId, e.getMessage(), e);
            throw new RuntimeException("Coze API 调用失败", e);
        }
    }

    @Override
    public String extractMessageContent(String responseRaw) {
        if (responseRaw == null || responseRaw.isBlank()) {
            return "";
        }
        try {
            return stream ? extractStreamContent(responseRaw) : extractNonStreamContent(responseRaw);
        } catch (Exception e) {
            log.warn("解析 Coze 响应内容失败: {}", e.getMessage());
            return responseRaw.length() > 500 ? responseRaw.substring(0, 500) + "..." : responseRaw;
        }
    }

    private Map<String, Object> buildRequestBody(String dynamicContent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", botId);
        body.put("user_id", userId);
        body.put("auto_save_history", autoSaveHistory);
        body.put("stream", stream);

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("type", "question");
        message.put("content_type", "text");
        message.put("content", dynamicContent != null ? dynamicContent : "");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message);
        body.put("additional_messages", messages);
        return body;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String buildApiUrl(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return apiUrl;
        }
        return apiUrl + (apiUrl.contains("?") ? "&" : "?") + "conversation_id=" + conversationId;
    }

    private String callNonStreamApi(long start, String dynamicContent, String conversationId) {
        String url = buildApiUrl(conversationId);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(buildRequestBody(dynamicContent), buildHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        String responseBody = response.getBody();

        checkCozeBusinessError(responseBody);
        log.info("Coze 非流式调用成功, conversationId={}, HTTP状态码={}, 耗时={}ms",
                conversationId, response.getStatusCode().value(), System.currentTimeMillis() - start);
        return responseBody;
    }

    private String callStreamApi(long start, String dynamicContent, String conversationId) throws IOException {
        String response = executeStreamRequest(buildApiUrl(conversationId), buildRequestBody(dynamicContent));
        checkCozeBusinessError(response);
        log.info("Coze 流式调用成功, conversationId={}, 数据长度={}, 耗时={}ms",
                conversationId, response.length(), System.currentTimeMillis() - start);
        return response;
    }

    private String executeStreamRequest(String urlStr, Map<String, Object> body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Coze API 返回错误: " + readErrorBody(connection, responseCode));
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String eventName = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                if (eventName != null && !eventName.isEmpty()) {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("event", eventName);
                    try {
                        wrapper.put("data", JSON.parseObject(data));
                    } catch (Exception ignored) {
                        wrapper.put("data", data);
                    }
                    result.append(JSON.toJSONString(wrapper)).append('\n');
                    eventName = null;
                } else {
                    result.append(data).append('\n');
                }
            }
        }
        return result.toString();
    }

    private String readErrorBody(HttpURLConnection connection, int responseCode) throws IOException {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream == null) {
                return "HTTP " + responseCode;
            }
            return new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractNonStreamContent(String responseRaw) {
        JSONObject root = JSON.parseObject(responseRaw);
        String directContent = root.getString("content");
        if (directContent != null && !directContent.isBlank()) {
            return directContent;
        }

        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            return "";
        }
        String dataContent = data.getString("content");
        if (dataContent != null && !dataContent.isBlank()) {
            return dataContent;
        }

        JSONArray messages = data.getJSONArray("messages");
        if (messages == null) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject message = messages.getJSONObject(i);
            if (message != null && ANSWER.equals(message.getString("type"))
                    && message.getString("content") != null) {
                return message.getString("content");
            }
        }
        return "";
    }

    private String extractStreamContent(String responseRaw) {
        StringBuilder answerBuilder = new StringBuilder();
        String followUpContent = null;
        String completedAnswer = null;

        for (String line : responseRaw.split("\n")) {
            try {
                JSONObject eventObject = JSON.parseObject(line);
                String event = eventObject.getString("event");
                JSONObject data = eventObject.getJSONObject("data");

                if (data != null) {
                    if ("conversation.message.delta".equals(event) && ANSWER.equals(data.getString("type"))) {
                        answerBuilder.append(data.getString("content"));
                    } else if ("conversation.message.completed".equals(event)) {
                        if (FOLLOW_UP.equals(data.getString("type"))) {
                            followUpContent = data.getString("content");
                        } else if (ANSWER.equals(data.getString("type"))) {
                            completedAnswer = data.getString("content");
                        }
                    }
                } else if (ANSWER.equals(eventObject.getString("type"))) {
                    answerBuilder.append(eventObject.getString("content"));
                } else if (FOLLOW_UP.equals(eventObject.getString("type"))) {
                    followUpContent = eventObject.getString("content");
                }
            } catch (Exception ignored) {
                // SSE 中可能包含 keep-alive 或非 JSON 数据。
            }
        }

        String answer = answerBuilder.length() > 0 ? answerBuilder.toString() : completedAnswer;
        Map<String, Object> result = new LinkedHashMap<>();
        if (answer != null && !answer.isBlank()) {
            result.put("answer", answer);
        }
        if (followUpContent != null && !followUpContent.isBlank()) {
            result.put("follow_up", followUpContent);
        }
        return result.isEmpty() ? "" : JSON.toJSONString(result);
    }

    private void checkCozeBusinessError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(responseBody);
            if (root != null && root.containsKey("code") && root.getIntValue("code") != 0) {
                throw new RuntimeException("Coze API 业务错误: code="
                        + root.getIntValue("code") + ", msg=" + root.getString("msg"));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
            // 流式响应由多段 JSON 组成，业务错误会在各事件中体现。
        }
    }
}
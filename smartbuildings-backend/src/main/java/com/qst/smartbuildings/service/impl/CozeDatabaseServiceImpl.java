package com.qst.smartbuildings.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qst.smartbuildings.entity.coze.CozeDocument;
import com.qst.smartbuildings.service.CozeDatabaseService;
import com.qst.smartbuildings.service.CozeDocumentService;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Coze 知识库（Database）服务实现。
 *
 * <p>负责调用 Coze Knowledge Database API（v1/databases），提供
 * 日报模板与报警规则提示词的查询与更新能力，以及知识库报告文档的定时同步能力。</p>
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

    @Value("${coze.database2.id}")
    private String database2Id;

    @Value("${coze.document.storage-path}")
    private String documentStoragePath;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private CozeDocumentService cozeDocumentService;

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

    /**
     * 同步 Coze 知识库（第二个数据库）中的文档到本地。
     *
     * <p>
     * 流程：
     * 1. 查询 Coze 知识库获取所有文档记录（doc_id, doc_name, bstudio_create_time, doc_url）
     * 2. 对比本地数据库已有的 doc_id，只处理新增的文档
     * 3. 下载 doc_url 文件到本地存储目录
     * 4. 将记录写入本地数据库，doc_url 替换为本地路径
     * </p>
     *
     * @return 同步结果统计字符串
     */
    @Override
    public String syncDocuments() {
        if (database2Id == null || database2Id.trim().isEmpty()) {
            return "同步已跳过: 未配置 coze.database2.id";
        }

        String queryUrl = databaseUrl + "/" + database2Id.trim() + "/records/query";

        // 构建请求体 — 查询全部，select 四个字段
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> selectFields = new LinkedHashMap<>();
        List<String> fieldNames = Arrays.asList("doc_id", "doc_name", "bstudio_create_time", "doc_url");
        selectFields.put("field_names", fieldNames);
        body.put("select_fields", selectFields);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, buildHeaders());

        log.info("Coze 文档同步: 开始查询知识库, url={}", queryUrl);

        ResponseEntity<String> response = restTemplate.exchange(
                queryUrl, HttpMethod.POST, requestEntity, String.class);
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            return "同步完成: 知识库无数据";
        }
        checkCozeBusinessError(responseBody);

        // 解析 items
        JSONObject root = JSON.parseObject(responseBody);
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            return "同步完成: 知识库无数据";
        }
        JSONArray items = data.getJSONArray("items");
        if (items == null || items.isEmpty()) {
            return "同步完成: 知识库无数据";
        }

        // 获取本地已有的 doc_id 集合
        List<String> existingDocIds = cozeDocumentService.listAllDocIds();
        Set<String> existingSet = new HashSet<>(existingDocIds);

        int total = items.size();
        int skipped = 0;
        int downloaded = 0;
        int failed = 0;

        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            String docName = item.getString("doc_name");
            String docUrl = item.getString("doc_url");
            String bstudioCreateTimeStr = item.getString("bstudio_create_time");

            // 优先使用 Coze 数据库记录 id；仅有业务 doc_id 时再回退使用 doc_id。
            // 不使用签名 doc_url 作为标识，避免签名参数变化导致重复同步。
            String docId = firstNonBlank(item.getString("id"), item.getString("doc_id"));
            if (docId == null) {
                String fingerprint = firstNonBlank(docName, "") + "|" + firstNonBlank(bstudioCreateTimeStr, "");
                if (!"|".equals(fingerprint)) {
                    docId = "fallback_" + Integer.toHexString(fingerprint.hashCode());
                }
            }
            if (docId == null || docUrl == null || docUrl.trim().isEmpty()) {
                failed++;
                log.warn("跳过无效文档记录: docName={}, 缺少记录ID或doc_url", docName);
                continue;
            }
            docId = docId.trim();

            // 增量判断：已存在则跳过
            if (existingSet.contains(docId)) {
                skipped++;
                continue;
            }

            Date bstudioCreateTime = parseCreateTime(bstudioCreateTimeStr);
            if (bstudioCreateTime == null && bstudioCreateTimeStr != null && !bstudioCreateTimeStr.trim().isEmpty()) {
                log.warn("解析 bstudio_create_time 失败: {}, docId={}", bstudioCreateTimeStr, docId);
            }

            // 下载成功后再写库；下载失败的记录不入库，下次同步会自动重试。
            String localDocUrl;
            try {
                localDocUrl = downloadDocument(docId, docName, docUrl);
            } catch (Exception e) {
                failed++;
                log.error("下载文档失败: docId={}, docUrl={}, error={}", docId, docUrl, e.getMessage(), e);
                continue;
            }

            try {
                CozeDocument doc = new CozeDocument();
                doc.setDocId(docId);
                doc.setDocName(docName);
                doc.setDocUrl(localDocUrl);
                doc.setBstudioCreateTime(bstudioCreateTime);
                cozeDocumentService.save(doc);
                existingSet.add(docId);
                downloaded++;
                log.info("文档同步成功: docId={}, docName={}", docId, docName);
            } catch (Exception e) {
                failed++;
                deleteQuietly(localDocUrl);
                log.error("文档入库失败: docId={}, docName={}, error={}", docId, docName, e.getMessage(), e);
            }
        }

        String result = String.format(
                "同步完成: 总计%d条, 新增%d条, 跳过%d条(已存在), 失败%d条",
                total, downloaded, skipped, failed);
        log.info("Coze 文档同步: {}", result);
        return result;
    }

    /**
     * 从 Coze 的 doc_url 下载文件到本地存储目录。
     *
     * @param docId  文档 ID，用作子目录名
     * @param docUrl Coze 提供的文档下载 URL
     * @return 本地文件相对路径（用于浏览器访问）
     * @throws Exception 下载或写入失败时抛出
     */
    private String downloadDocument(String docId, String docName, String docUrl) throws Exception {
        File baseDir = new File(documentStoragePath);
        if (!baseDir.isAbsolute()) {
            baseDir = new File(System.getProperty("user.dir"), documentStoragePath);
        }

        String storageKey = safeStorageKey(docId);
        Path dir = Files.createDirectories(baseDir.toPath().resolve(storageKey));
        String fileName = "original" + inferFileExtension(docUrl, docName);
        Path localFile = dir.resolve(fileName);
        Path tempFile = dir.resolve(fileName + ".part");

        HttpURLConnection conn = (HttpURLConnection) new URL(docUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("User-Agent", "SmartBuildings-Coze-Sync/1.0");
        conn.setRequestProperty("Accept", "*/*");

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("下载响应异常: HTTP " + responseCode);
            }
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, localFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
            Files.deleteIfExists(tempFile);
        }

        // 返回相对路径（与 WebConfig 中 /doc-files/ 资源映射对应）
        return storageKey + "/" + fileName;
    }

    /**
     * 解析 Coze 记录的创建时间，兼容秒/毫秒时间戳、ISO 时间和常见日期格式。
     */
    private Date parseCreateTime(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        String value = rawValue.trim();
        try {
            if (value.matches("\\d{10}")) {
                return Date.from(Instant.ofEpochSecond(Long.parseLong(value)));
            }
            if (value.matches("\\d{13}")) {
                return Date.from(Instant.ofEpochMilli(Long.parseLong(value)));
            }
            if (value.endsWith("Z")) {
                return Date.from(Instant.parse(value));
            }
            try {
                return Date.from(OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
            } catch (Exception ignored) {
                // 尝试无时区的 LocalDateTime 格式
            }
            DateTimeFormatter[] isoFormatters = new DateTimeFormatter[]{
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            };
            for (DateTimeFormatter formatter : isoFormatters) {
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
                    return Date.from(localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
                } catch (Exception ignored) {
                    // 继续尝试下一种格式
                }
            }
        } catch (Exception ignored) {
            // 使用下方 SimpleDateFormat 兼容更老的返回格式
        }

        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.ROOT);
                formatter.setLenient(false);
                return formatter.parse(value);
            } catch (Exception ignored) {
                // 继续尝试下一种格式
            }
        }
        return null;
    }

    private String inferFileExtension(String docUrl, String docName) {
        String[] candidates = {docUrl, docName};
        for (String candidate : candidates) {
            if (candidate == null || candidate.trim().isEmpty()) {
                continue;
            }
            String clean = candidate;
            int queryIndex = clean.indexOf('?');
            if (queryIndex >= 0) {
                clean = clean.substring(0, queryIndex);
            }
            int fragmentIndex = clean.indexOf('#');
            if (fragmentIndex >= 0) {
                clean = clean.substring(0, fragmentIndex);
            }
            int dotIndex = clean.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < clean.length() - 1) {
                String extension = clean.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|md|png|jpg|jpeg|gif|zip|rar|json|csv|xml|html?)$")) {
                    return extension;
                }
            }
        }
        return "";
    }

    private String safeStorageKey(String docId) {
        String safe = docId == null ? "" : docId.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isEmpty() || ".".equals(safe) || "..".equals(safe)) {
            safe = "doc_" + Integer.toHexString(String.valueOf(docId).hashCode());
        }
        if (safe.length() > 100) {
            safe = safe.substring(0, 80) + "_" + Integer.toHexString(safe.hashCode());
        }
        return safe;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }

    private void deleteQuietly(String localDocUrl) {
        if (localDocUrl == null || localDocUrl.trim().isEmpty()) {
            return;
        }
        try {
            File baseDir = new File(documentStoragePath);
            if (!baseDir.isAbsolute()) {
                baseDir = new File(System.getProperty("user.dir"), documentStoragePath);
            }
            Files.deleteIfExists(baseDir.toPath().resolve(localDocUrl));
        } catch (Exception ignored) {
            // 清理失败不影响同步结果统计
        }
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

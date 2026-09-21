package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.coze.CozeChatConversation;
import com.qst.smartbuildings.service.CozeChatConversationService;
import com.qst.smartbuildings.service.CozeChatService;
import com.qst.smartbuildings.service.CozeConversationService;
import com.qst.smartbuildings.util.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coze 会话管理接口。
 */
@RestController
@RequestMapping("/coze/conversation")
@Tag(name = "Coze 会话管理", description = "Coze 智能监测助手的会话与对话接口")
public class CozeConversationController {

    @Autowired
    private CozeConversationService conversationService;

    @Autowired
    private CozeChatService chatService;

    @Autowired
    private CozeChatConversationService conversationDbService;

    @PostMapping("/create")
    @Operation(summary = "创建会话", description = "调用 Coze 创建会话接口，并写入本地会话记录")
    public Result<Map<String, Object>> createConversation(@RequestBody(required = false) Map<String, Object> params) {
        String name = getText(params, "name");
        try {
            Map<String, Object> cozeResult = conversationService.createConversation(name);
            String conversationId = (String) cozeResult.get("conversationId");
            if (conversationId != null) {
                Long currentUserId = UserUtil.getCurrentUserId();
                if (currentUserId != null) {
                    CozeChatConversation local = conversationDbService.saveConversation(
                            currentUserId, conversationId, name);
                    cozeResult.put("localId", local.getId());
                }
            }
            return Result.ok(cozeResult);
        } catch (Exception e) {
            return Result.error(500, "创建会话失败: " + e.getMessage());
        }
    }

    @PostMapping("/retrieve")
    @Operation(summary = "查看会话详情", description = "根据 conversation_id 查询 Coze 会话基本信息")
    public Result<Map<String, Object>> retrieveConversation(@RequestBody Map<String, Object> params) {
        String conversationId = getText(params, "conversationId");
        if (conversationId == null) {
            return Result.error(400, "conversationId 不能为空");
        }
        try {
            return Result.ok(conversationService.retrieveConversation(conversationId));
        } catch (Exception e) {
            return Result.error(500, "查看会话失败: " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "在会话中发起对话", description = "基于 conversation_id 向 Coze Bot 发送消息")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
        String conversationId = getText(params, "conversationId");
        if (conversationId == null) {
            return Result.error(400, "conversationId 不能为空");
        }
        String message = getText(params, "message");
        if (message == null) {
            return Result.error(400, "message 不能为空");
        }

        try {
            String response = chatService.callCozeApiWithConversation(message, conversationId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("conversationId", conversationId);
            result.put("message", message);
            result.put("responseRaw", response);
            result.put("answer", chatService.extractMessageContent(response));
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(500, "会话对话失败: " + e.getMessage());
        }
    }

    @PostMapping("/list")
    @Operation(summary = "查询当前用户的会话列表", description = "从本地数据库查询当前用户的所有会话记录")
    public Result<List<Map<String, Object>>> listConversations() {
        Long currentUserId = UserUtil.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "未登录");
        }
        try {
            List<Map<String, Object>> result = conversationDbService.listByUserId(currentUserId)
                    .stream()
                    .map(this::toConversationMap)
                    .collect(Collectors.toList());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error(500, "查询会话列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    @Operation(summary = "删除会话", description = "从本地数据库删除指定会话记录")
    public Result<Boolean> deleteConversation(@RequestBody Map<String, Object> params) {
        Object idValue = params == null ? null : params.get("id");
        Long id = idValue instanceof Number ? ((Number) idValue).longValue() : null;
        if (id == null) {
            return Result.error(400, "id 不能为空");
        }
        try {
            boolean deleted = conversationDbService.deleteById(id);
            return deleted ? Result.ok(true) : Result.error(404, "会话不存在");
        } catch (Exception e) {
            return Result.error(500, "删除会话失败: " + e.getMessage());
        }
    }

    private String getText(Map<String, Object> params, String key) {
        Object value = params == null ? null : params.get(key);
        if (!(value instanceof String)) {
            return null;
        }
        String text = ((String) value).trim();
        return text.isEmpty() ? null : text;
    }

    private Map<String, Object> toConversationMap(CozeChatConversation conversation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", conversation.getId());
        item.put("conversationId", conversation.getConversationId());
        item.put("title", conversation.getTitle());
        item.put("createdAt", conversation.getCreatedAt());
        item.put("updatedAt", conversation.getUpdatedAt());
        return item;
    }
}
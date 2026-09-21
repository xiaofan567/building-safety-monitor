package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.service.CozeDatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Coze 知识库（Database）管理接口。
 *
 * <p>对外暴露日报模板与报警规则的查询/更新能力，供前端模板编辑与规则配置页面使用。</p>
 */
@RestController
@RequestMapping("/coze/database")
@Tag(name = "Coze 知识库管理", description = "Coze 知识库 Database 表的模板与规则查询/更新接口")
public class CozeDatabaseController {

    @Autowired
    private CozeDatabaseService cozeDatabaseService;

    @PostMapping("/getTemplate")
    @Operation(summary = "获取日报模板", description = "从 Coze 知识库查询日报模板内容（prompt_key = 'doc_template'）")
    public Result<Map<String, String>> getTemplate() {
        try {
            String promptText = cozeDatabaseService.queryRecord("doc_template");
            return Result.ok(Collections.singletonMap("promptText", promptText));
        } catch (Exception e) {
            return Result.error(500, "获取模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/updateTemplate")
    @Operation(summary = "更新日报模板", description = "将日报模板内容更新到 Coze 知识库（prompt_key = 'doc_template'）")
    public Result<String> updateTemplate(@RequestBody Map<String, String> params) {
        String promptText = params.get("promptText");
        if (promptText == null || promptText.isEmpty()) {
            return Result.error(400, "promptText 不能为空");
        }
        try {
            cozeDatabaseService.updateTemplate(promptText);
            return Result.ok("模板更新成功");
        } catch (Exception e) {
            return Result.error(500, "模板更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/updateAlertConfig")
    @Operation(summary = "更新报警配置", description = "将报警规则内容更新到 Coze 知识库（prompt_key = 'rule_template'）")
    public Result<String> updateAlertConfig(@RequestBody Map<String, String> params) {
        String promptText = params.get("promptText");
        if (promptText == null || promptText.isEmpty()) {
            return Result.error(400, "promptText 不能为空");
        }
        try {
            cozeDatabaseService.updateAlertConfig(promptText);
            return Result.ok("报警配置更新成功");
        } catch (Exception e) {
            return Result.error(500, "报警配置更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/getAlertConfig")
    @Operation(summary = "获取报警配置", description = "从 Coze 知识库查询报警规则内容（prompt_key = 'rule_template'）")
    public Result<Map<String, String>> getAlertConfig() {
        try {
            String promptText = cozeDatabaseService.queryRecord("rule_template");
            return Result.ok(Collections.singletonMap("promptText", promptText));
        } catch (Exception e) {
            return Result.error(500, "获取报警配置失败: " + e.getMessage());
        }
    }
}
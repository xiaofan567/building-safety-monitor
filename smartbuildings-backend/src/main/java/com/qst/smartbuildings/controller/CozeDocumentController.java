package com.qst.smartbuildings.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.coze.CozeDocument;
import com.qst.smartbuildings.service.CozeDatabaseService;
import com.qst.smartbuildings.service.CozeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coze 知识库文档查询接口。
 *
 * <p>对外暴露本地同步的文档列表查询与手动触发同步能力，供前端日报查看页面使用。</p>
 */
@RestController
@RequestMapping("/document")
@Tag(name = "文档查询", description = "Coze 知识库文档的本地分页查询与同步接口")
public class CozeDocumentController {

    @Autowired
    private CozeDocumentService cozeDocumentService;

    @Autowired
    private CozeDatabaseService cozeDatabaseService;

    private static final Logger log = LoggerFactory.getLogger(CozeDocumentController.class);

    @PostMapping("/list")
    @Operation(summary = "查询文档列表", description = "按文档名称和日期范围分页查询本地同步的文档")
    public Result list(@RequestBody Map<String, Object> params) {
        int pageNum = params.get("page") instanceof Number
                ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params.get("limit") instanceof Number
                ? ((Number) params.get("limit")).intValue() : 10;
        String docName = params.get("docName") instanceof String
                ? (String) params.get("docName") : null;
        String startDate = params.get("startDate") instanceof String
                ? (String) params.get("startDate") : null;
        String endDate = params.get("endDate") instanceof String
                ? (String) params.get("endDate") : null;

        IPage<CozeDocument> pageResult = cozeDocumentService.selectDocumentList(
                pageNum, pageSize, docName, startDate, endDate);

        List<CozeDocument> records = pageResult.getRecords();
        List<Map<String, Object>> docList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (CozeDocument doc : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", doc.getId());
            item.put("docId", doc.getDocId());
            item.put("docName", doc.getDocName());
            item.put("docUrl", doc.getDocUrl());
            item.put("createTime", doc.getBstudioCreateTime() != null
                    ? sdf.format(doc.getBstudioCreateTime()) : "");
            docList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", docList);
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("limit", pageResult.getSize());

        return Result.ok(result);
    }

    @PostMapping("/sync")
    @Operation(summary = "手动同步文档", description = "立即触发一次 Coze 知识库文档同步，便于前端手动刷新")
    public Result<String> sync() {
        try {
            String result = cozeDatabaseService.syncDocuments();
            log.info("手动触发文档同步: {}", result);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("手动同步文档失败: {}", e.getMessage(), e);
            return Result.error(500, "同步失败: " + e.getMessage());
        }
    }
}
package com.qst.smartbuildings.controller.camera;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.entity.camera.param.AiAlertParam;
import com.qst.smartbuildings.service.AiAlertService;
import com.qst.smartbuildings.util.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/aiAlert")
@Tag(name = "监测报警记录",description = "监测报警记录的查询、修改、删除等操作接口")
public class AiAlertController {
    @Autowired
    private AiAlertService aiAlertService;
    @PostMapping("/selectPage")
    @Operation(summary = "查询AI摄像头报警信息列表", description = "根据查询条件查询AI摄像头报警信息列表")
    public Result<BPage<AiAlert>> selectPage(@RequestBody AiAlertParam query){
        return Result.ok(aiAlertService.selectPage(query));
    }
    @PostMapping("/get/{id}")
    @Operation(summary = "查询报警详情", description = "根据ID查询报警的详细信息")
    public Result<AiAlert> get(
            @Parameter(description = "报警ID", required = true)
            @PathVariable("id") Integer id) {
        AiAlert aiAlert = aiAlertService.getAiAlertById(id);
        return Result.ok(aiAlert);
    }
    @PostMapping("/update")
    public Result update(@RequestBody AiAlert aiAlert){
        aiAlert.setUpdateTime(new Date());
        aiAlert.setUpdateUserId(UserUtil.getCurrentUserId());
        aiAlert.setHandleTime(new Date());
        aiAlertService.updateById(aiAlert);
        return Result.ok();
    }
}

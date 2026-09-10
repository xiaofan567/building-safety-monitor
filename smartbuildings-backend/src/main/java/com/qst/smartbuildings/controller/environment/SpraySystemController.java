package com.qst.smartbuildings.controller.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.environment.SprayTask;
import com.qst.smartbuildings.entity.param.SpraySystemParam;
import com.qst.smartbuildings.entity.param.SprayTaskParam;
import com.qst.smartbuildings.service.environment.SpraySystemService;
import com.qst.smartbuildings.service.environment.SprayTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 自动喷淋管理控制器
 *
 * @author system
 * @since 1.0
 */
@RestController
@RequestMapping("/spraySystem")
@Tag(name = "自动喷淋管理", description = "自动喷淋记录查询，自动喷淋配置增删改查操作")
public class SpraySystemController {

    @Autowired
    private SpraySystemService spraySystemService;

    @Autowired
    private SprayTaskService sprayTaskService;

    /**
     * 自动喷淋记录查询
     *
     * @param spraySystemParam 查询条件
     * @return 分页结果
     */
    @RequestMapping("/list")
    @Operation(summary = "自动喷淋记录查询", description = "根据条件查询自动喷淋记录列表")
    public Result<BPage<SpraySystem>> list(@RequestBody SpraySystemParam spraySystemParam) {
        return Result.ok(spraySystemService.selectByPage(spraySystemParam));
    }

    /**
     * 喷淋任务配置查询
     *
     * @param sprayTaskParam 查询条件
     * @return 分页结果
     */
    @RequestMapping("/taskList")
    @Operation(summary = "喷淋任务配置查询", description = "根据条件查询喷淋任务配置列表")
    public Result<BPage<SprayTask>> taskList(@RequestBody SprayTaskParam sprayTaskParam) {
        return Result.ok(sprayTaskService.selectTaskByPage(sprayTaskParam));
    }

    /**
     * 添加或修改喷淋配置
     *
     * @param sprayTask 喷淋任务配置
     * @return 操作结果
     */
    @RequestMapping("/update")
    @Operation(summary = "添加或修改喷淋配置", description = "添加或修改喷淋配置")
    public Result<Boolean> save(@RequestBody SprayTask sprayTask) {
        if (sprayTask.getNextTime() == null) {
            sprayTask.setNextTime(sprayTask.getStartTime());
        }
        return Result.ok(sprayTaskService.saveOrUpdate(sprayTask));
    }

    /**
     * 删除喷淋任务配置
     *
     * @param sprayTask 喷淋任务配置
     * @return 操作结果
     */
    @RequestMapping("/delete")
    @Operation(summary = "删除喷淋任务配置", description = "根据id删除喷淋任务配置")
    public Result<Boolean> delete(@RequestBody SprayTask sprayTask) {
        return Result.ok(sprayTaskService.removeById(sprayTask.getId()));
    }
}

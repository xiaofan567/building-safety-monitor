package com.qst.smartbuildings.controller.camera;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.CameraParam;
import com.qst.smartbuildings.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/camera")
@Tag(name = "摄像头",description = "摄像头的查询、修改、删除等操作接口")
public class CameraController {
    @Autowired
    private CameraService cameraService;
    /**
     * 查询摄像头列表信息
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "查询摄像头列表信息")
    public Result list(@RequestBody CameraParam cameraParam){
        BPage<Camera> list = cameraService.selectByPage(cameraParam);
        return Result.ok(list);
    }
    @PostMapping("/get/{id}")
    @Operation(summary = "查询摄像头详情", description = "根据ID查询摄像头的详细信息")
    public Result<Camera> get(
            @Parameter(description = "设备类型ID", required = true)
            @PathVariable("id") Integer id) {
        Camera camera = cameraService.getCameraById(id);
        return Result.ok(camera);
    }
    @PostMapping("/update")
    @Operation(summary = "修改摄像头信息", description = "根据ID修改摄像头的详细信息")
    public Result updateById(
            @Parameter(description = "摄像头对象", required = true)
            @RequestBody Camera camera) {

        if(camera.getId() == null){
            return Result.error(500,"摄像头ID不能为空");
        }
        cameraService.updateById(camera);
        return Result.ok();
    }

}

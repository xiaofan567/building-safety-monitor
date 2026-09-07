package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.CameraParam;

public interface CameraService extends IService<Camera> {
    BPage<Camera> selectByPage(CameraParam cameraParam);

    Camera getCameraById(Integer id);
}

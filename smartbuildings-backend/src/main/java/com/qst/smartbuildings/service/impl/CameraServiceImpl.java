package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.CameraParam;
import com.qst.smartbuildings.mapper.CameraMapper;
import com.qst.smartbuildings.service.CameraService;
import org.springframework.stereotype.Service;

@Service
public class CameraServiceImpl extends ServiceImpl<CameraMapper, Camera> implements CameraService {

    @Override
    public BPage<Camera> selectByPage(CameraParam cameraParam) {
        return getBaseMapper().selectByPage(cameraParam.parse(),cameraParam);
    }

    @Override
    public Camera getCameraById(Integer id) {
        return getBaseMapper().selectCameraById(id);
    }
}

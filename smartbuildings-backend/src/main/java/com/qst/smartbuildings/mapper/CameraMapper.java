package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.CameraParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CameraMapper extends BaseMapper<Camera> {
    BPage<Camera> selectByPage(BPage<Camera> parse, @Param("data") CameraParam cameraParam);
    Camera selectCameraById(Integer id);
}

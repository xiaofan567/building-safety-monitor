package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.AIDetectionResult;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.AiAlertParam;

import java.awt.image.BufferedImage;
import java.util.List;

public interface AiAlertService extends IService<AiAlert> {
    BPage<AiAlert> selectPage(AiAlertParam aiAlertParam);
    public Integer insertByCamera(List<AIDetectionResult> results, Camera camera, BufferedImage image);

    AiAlert getAiAlertById(Integer id);
}

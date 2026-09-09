package com.qst.smartbuildings.aicameraserver;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qst.smartbuildings.common.UpdateOperationEnum;
import com.qst.smartbuildings.entity.camera.AIDetectionResult;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.exception.CameraException;
import com.qst.smartbuildings.mapper.CameraMapper;
import jakarta.annotation.PostConstruct;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.qst.smartbuildings.entity.camera.AIDetectionResult.checkDetectionResult;
import static com.qst.smartbuildings.entity.camera.AIDetectionResult.isValidDetectionResult;

/**
 * AI 摄像头服务
 * <p>
 * 负责服务初始化、摄像头直播流拉取与 AI 检测调度、上下线状态管理、
 * 失败摄像头定时重试等核心功能。
 * </p>
 *
 * @author building-safety-monitor
 */
@Component
public class AICameraService {
    private static final Logger log = LoggerFactory.getLogger(AICameraService.class);

    @Autowired
    private CameraMapper cameraMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private com.qst.smartbuildings.service.AiAlertService aiAlertService;
    //AI监测服务URL
    @Value("${camera.ai.baseUrl}")
    private String AI_CAMERA_URL;
    //直播摄像头URL
    @Value("${camera.live.baseUrl}")
    private String CAMERA_LIVE_URL;
    //活跃摄像头Map
    private final Map<Integer, Camera> liveCameras = new ConcurrentHashMap<>();
    //失败摄像头Map
    private final Map<Integer, Camera> failedCameras = new ConcurrentHashMap<>();
    //初始化线程，防止通过SpringBoot主线程初始化导致启动卡顿
    private Thread cameraServerThread;
    //摄像头线程Map，管理摄像头生命周期，key为摄像头ID，value为Future对象
    private Map<Integer, Future<?>> futureMap = new ConcurrentHashMap<>();
    /*
     * 初始化摄像头服务，查询数据库，获取状态为启用，且有监测类型的摄像头，然后启动摄像头切帧和AI检测功能
     * */
    @PostConstruct
    public void init() {
        cameraServerThread = new Thread(() -> {
//            查询可用摄像头，即状态为启用，且有监测类型和推流地址的摄像头
            QueryWrapper<Camera> wrapper = new QueryWrapper<Camera>()
                    .eq("is_enable", "1")
                    .isNotNull("wvp_open_url")
                    .isNotNull("ai_monitor_type");
            List<Camera> cameras = cameraMapper.selectList(wrapper);
//            摄像头循环初始化
            cameras.forEach(camera -> {
                log.info("初始化摄像头，摄像头ID：{}，状态：{}", camera.getId(), camera.getStatus());
                Future<?> future = startAICamera(camera);
                futureMap.put(camera.getId(), future);
            });
        });
        cameraServerThread.start();
    }
    /*
     * 启动摄像头，打开直播流，并每一秒截取视频帧，发送给AI服务进行检测
     * @param camera 摄像头实体
     * */
    // 配置线程池，防止摄像头线程阻塞主线程
    @Async("CameraProcessTaskExecutor")
    Future<?>  startAICamera(Camera camera) {
//        异步执行任务
        return CompletableFuture.runAsync(()->{
            // 拼出直播流URL nginx直播地址+wvp视频播放地址+index.m3u8
            String streamUrl = CAMERA_LIVE_URL + camera.getWvpOpenUrl() + "/index.m3u8";
            // 配置获取直播流参数，最多重试3次
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl);
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setFormat("hls");
            Java2DFrameConverter converter = new Java2DFrameConverter();
            // 记录上一次处理图像的时间戳
            long lastProcessTime = 0;
            // 时间间隔阈值(毫秒) 每5秒钟截取一帧
            long intervalThreshold = 5000;
            // 重试次数（移到循环外部）
            int retryCount = 0;
            // 最大重试次数
            final int MAX_RETRY_COUNT = 3;
            // 线程中断标志
            boolean isInterrupted = false;

            try {
                // 开始获取直播流
                grabber.setTimeout(1000);
                grabber.start();
                // 循环获取直播流帧，直到线程被中断
                while (!isInterrupted && !Thread.currentThread().isInterrupted()) {
                    Frame frame = null;
                    try {
//                        抓取一个视频帧，由于直播流帧为连续的，所有的帧必须都抓一下，否则帧队列会不前进，导致后续的帧无法处理
                        frame = grabber.grab();
                    } catch (Exception e) {
                        log.error("抓取帧异常，摄像头ID：{}", camera.getId(), e);
                        retryCount++;
                        if (retryCount > MAX_RETRY_COUNT) {
                            throw new CameraException("超过最大重试次数，摄像头ID：" + camera.getId());
                        }
                        continue;
                    }

                    long currentTime = System.currentTimeMillis();
                    // 检查是否距离上一次处理已经超过5秒
                    if (currentTime - lastProcessTime > intervalThreshold) {
                        if (frame == null) {
//                        log.error("截取到图片为空，摄像头ID：{}", camera.getId());
                            retryCount++;
                            if (retryCount > MAX_RETRY_COUNT) {
                                throw new CameraException("最大重试次数失败，销毁线程，摄像头ID：" + camera.getId());
                            }
                        } else if (frame.image != null) {
//                        log.info("截取到图片并处理，摄像头ID：{}", camera.getId());
                            try {
                                BufferedImage image = converter.convert(frame);
                                // 更新上一次处理时间
                                lastProcessTime = currentTime;
                                // 发送图片给AI服务进行检测
                                sendImageToDetectionService(image, camera);
                                setStatus(camera, "1");
                                // 成功处理后重置重试计数
                                retryCount = 0;
                            } catch (Exception e) {
                                log.error("处理图像异常，摄像头ID：{}", camera.getId(), e);
                                retryCount++;
                            }
                        }
                    }
                }
            } catch (FFmpegFrameGrabber.Exception e) {  // 显式捕获FFmpeg相关异常
                log.error("FFmpeg处理异常，摄像头ID：{}", camera.getId(), e);
            } catch (CameraException e) {
                log.error("处理摄像头流异常，摄像头ID：{}", camera.getId(), e);
            } catch (Exception e) {
                log.error("未知异常，摄像头ID：{}", camera.getId(), e);
            } catch (Throwable t) {  // 捕获所有Throwable
                log.error("捕获到Error或Exception，摄像头ID：{}", camera.getId(), t);
            } finally {
                log.info("摄像头线程已结束，摄像头ID：{}", camera.getId());
                setStatus(camera, "0");
                if (grabber != null) {
                    try {
                        grabber.stop();
                        grabber.release(); // 释放资源
                        log.info("摄像头流抓取器已关闭，摄像头ID：{}", camera.getId());
                    } catch (Exception e) {
                        log.error("关闭抓取器失败，摄像头ID：{}", camera.getId(), e);
                    }
                }
                //移除线程管理
                futureMap.remove(camera.getId());
            }
        });
    }
    //设置摄像头上下线状态，0为下线，1为上线
    private void setStatus(Camera camera, String status) {
        if (!status.equals(camera.getStatus())) {
            camera.setStatus(status);
            cameraMapper.updateById(camera);
            log.info("更新摄像头状态，摄像头ID：{}，新状态：{}", camera.getId(), status);
        }
        if ("1".equals(status)&&!liveCameras.containsKey(camera.getId())) {
            liveCameras.put(camera.getId(), camera);
            failedCameras.remove(camera.getId());
        } else if ("0".equals(status)&&!failedCameras.containsKey(camera.getId())){
            liveCameras.remove(camera.getId());
            failedCameras.put(camera.getId(), camera);
        }
    }
    /*
     * 定时任务，每30秒尝试重新启动失败的摄像头
     * */
    @Scheduled(fixedRate = 30000)
    public void reloadFailedCameras() {
        log.info("定时任务，重新启动失败的摄像头,共有 {}个摄像头待尝试重启",failedCameras.size());
        failedCameras.values().forEach(camera -> {
            Future<?> future = startAICamera(camera);
            futureMap.put(camera.getId(), future);
        });
    }
    public void  updateCamera(Camera camera, UpdateOperationEnum operationEnum) {
        //不管如何操作，都先移除原有记录
        //在正式更新前，首先判断实际要走的操作，主要是在更新操作判断，
        //如果之前摄像头无效，现在有效，则走添加操作
        //如果之前摄像头有效，现在无效，则走删除操作
        //如果均有效，则正常走更新
        //如果均无效，则直接跳过
        boolean isBeforeValid = liveCameras.containsKey(camera.getId())||failedCameras.containsKey(camera.getId());
        boolean isAfterValid = isCameraCanUse(camera);
        if(!isBeforeValid&&isAfterValid){
            operationEnum = UpdateOperationEnum.ADD;
        }else if(isBeforeValid&&!isAfterValid){
            operationEnum = UpdateOperationEnum.DELETE;
        }else if(!isBeforeValid&&!isAfterValid){
            return;
        }
        if (operationEnum == UpdateOperationEnum.ADD) {
            //如果启用状态为1，且AI检测类型不为空，且直播流不为空，则可以启用摄像头
            if(isCameraCanUse(camera)) {
                //加入失败队列，下次重启时启动
                failedCameras.put(camera.getId(), camera);
            }
            log.info("摄像头新增，在下次重启摄像头时生效，摄像头ID：{}", camera.getId());
        } else if (operationEnum == UpdateOperationEnum.UPDATE) {
            //走更新操作，先判断摄像头线程是否在执行，如果在执行则停掉，如果不执行则直接更新faildMap即可
            Future<?> future = futureMap.get(camera.getId());
            if(future!=null) {
                future.cancel(true);
            }
            //下次重启时生效
            liveCameras.remove(camera.getId());
            failedCameras.put(camera.getId(), camera);
            log.info("摄像头更新，在下次重启摄像头时生效，摄像头ID：{}", camera.getId());
        } else if (operationEnum == UpdateOperationEnum.DELETE) {
            //删除摄像头时，先判断摄像头线程是否在执行，如果在执行则停掉，如果不执行则直接从liveCameras和failedCameras中移除即可
            Future<?> future = futureMap.get(camera.getId());
            if(future!=null) {
                future.cancel(true);
            }
            liveCameras.remove(camera.getId());
            failedCameras.remove(camera.getId());
            log.info("摄像头删除，摄像头ID：{}",camera.getId());
        }
    }
    /*
     * 检查摄像头是否可以进行AI监测功能
     * 要求摄像头处于启用状态，且AI检测类型不为空，且直播流不为空
     * @param camera 摄像头实体
     * @return true 可以使用，false 不能使用
     * */
    public Boolean isCameraCanUse(Camera camera) {
        return "1".equals(camera.getIsEnable())&& ObjectUtil.isNotEmpty(camera.getAiMonitorType())&&ObjectUtil.isNotEmpty(camera.getWvpOpenUrl());
    }

    //AI检测结果缓存，用于存储摄像头ID和最新的AI检测结果
    private Map<Integer, List<AIDetectionResult>> aiDetectionResultMap = new HashMap<>();

    /*
     * 发送图片给AI服务进行检测
     * @param image 图片
     * @param camera 摄像头实体
     * */
    private void sendImageToDetectionService(BufferedImage image, Camera camera) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            // 构建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "frame.jpg";
                }
            });
            // 添加检测类型 例如1,2,3
            String[] types = camera.getAiMonitorType().split(",");
            for (String type : types) {
                body.add("type", type);
            }
            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            String detectUrl = AI_CAMERA_URL;
            ResponseEntity<String> response = restTemplate.postForEntity(detectUrl, request, String.class);
            // 解析响应列表
            List<AIDetectionResult> detectionResults = JSONObject.parseArray(response.getBody(), AIDetectionResult.class);
            // 处理检测结果,首先检测是否为有效报警目标
            if (isValidDetectionResult(detectionResults)) {
                // 处理有效检测结果
                List<AIDetectionResult> oldList = aiDetectionResultMap.get(camera.getId());
                // 检查是否有变化
                if (checkDetectionResult(oldList, detectionResults)) {
                    // 发送报警
                    aiAlertService.insertByCamera(detectionResults, camera, image);
                }
            }
            //将最新的监测结果存入到缓存中
            aiDetectionResultMap.put(camera.getId(), detectionResults);
        } catch (IOException e) {
            log.error("图片转换失败", e);
            throw new RuntimeException("图片转换失败", e);
        } catch (RestClientException e) {
            log.error("请求检测服务失败", e);
            throw new RuntimeException("请求检测服务失败", e);
        }
    }
}
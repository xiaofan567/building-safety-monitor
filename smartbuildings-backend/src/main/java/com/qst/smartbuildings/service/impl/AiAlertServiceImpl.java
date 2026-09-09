package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.camera.AIDetectionResult;
import com.qst.smartbuildings.entity.camera.AiAlert;
import com.qst.smartbuildings.entity.camera.Camera;
import com.qst.smartbuildings.entity.camera.param.AiAlertParam;
import com.qst.smartbuildings.mapper.AiAlertMapper;
import com.qst.smartbuildings.service.AiAlertService;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

@Service
public class AiAlertServiceImpl extends ServiceImpl<AiAlertMapper, AiAlert> implements AiAlertService {
    @Autowired
    private RedisClient redisClient;

    @Override
    public BPage<AiAlert> selectPage(AiAlertParam aiAlertParam) {
        return getBaseMapper().selectPage(aiAlertParam.parse(),aiAlertParam);
    }
    Font font;
    @PostConstruct
    public void init() throws IOException {
        String FONT_PATH = "fonts/AlibabaPuHuiTi-2-55-Regular.otf";

        // 使用Spring的资源加载器读取classpath下的字体文件
        Resource resource = new ClassPathResource(FONT_PATH);

        // 验证文件是否存在
        if (!resource.exists()) {
            throw new IOException("字体文件不存在: " + FONT_PATH);
        }
        // 读取字体文件流
        try (InputStream is = resource.getInputStream()) {
            // 创建字体
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            // 设置字体大小并返回
            font = baseFont.deriveFont(Font.BOLD, 14);
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException("字体加载失败", e);
        }
    }

    @Override
    public Integer insertByCamera(List<AIDetectionResult> results, Camera camera, BufferedImage image) {
        AiAlert aiAlert = new AiAlert();
        aiAlert.setAlertTime(new Date());
        aiAlert.setAlertStatus("1");
        aiAlert.setEquipmentId(camera.getAssetId());
        aiAlert.setCreateTime(new Date());
        aiAlert.setUpdateTime(new Date());
        for (AIDetectionResult result : results) {
            if(result.getValidCount()>0){
                aiAlert.addAlertType(String.valueOf(result.getType()));
                aiAlert.addAlertContent(String.format("检测到异常目标，异常类型：%s,检测人数:%d",result.getTypeName(),result.getValidCount()));
            }
        }
        // 生成检测图片
        if(image!=null){
            try {
                String fileName = generateDetectionImage(results, image);
                aiAlert.setAlertImg(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int i = getBaseMapper().insert(aiAlert);

        return i;
    }

    @Override
    public AiAlert getAiAlertById(Integer id) {
        return getBaseMapper().getAiAlertById(id);
    }

    /*
    生成检测图片
     */
    public String generateDetectionImage(List<AIDetectionResult> results, BufferedImage image) throws IOException {
        // 获取项目根目录路径（与src同级）
        String projectRoot = System.getProperty("user.dir");
        // 构建 ai_image 目录路径
        Path aiImageDir = Paths.get(projectRoot, "ai_image");
        // 确保目录存在，不存在则创建
        if (!Files.exists(aiImageDir)) {
            Files.createDirectories(aiImageDir);
        }
        // 绘制检测框
        for (AIDetectionResult result : results) {
            if (result.getValidCount()>0){
                image = drawDetectionBoxes(image, result.getBoxes(), result.getTypeName());
            }
        }
        //随机生成图片名
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + ".jpg";

        //保存图片
        try {
            ImageIO.write(image, "jpg", new File(aiImageDir.toFile(),fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }
    /**
     * 在图像上绘制多个检测框和对应的标签
     * @param originalImage 原始图像
     * @param detectionBoxes 检测框列表
     * @param label 检测框对应的标签文本（如"目标"、"人脸"等）
     * @return 绘制后的图像
     */
    public BufferedImage drawDetectionBoxes(BufferedImage originalImage,
                                            List<AIDetectionResult.DetectionBox> detectionBoxes,
                                            String label) {
        // 创建可绘制的图像副本
        BufferedImage image = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        // 获取绘图上下文
        Graphics2D g2d = image.createGraphics();
        // 启用抗锯齿，使绘制的线条和文字更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // 先绘制原始图像
        g2d.drawImage(originalImage, 0, 0, null);
        // 设置绘制属性
        // 检测框线条：红色，2px粗
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        // 标签字体：Arial，粗体，14号
        g2d.setFont(font);
        // 遍历所有检测框并绘制
        if (detectionBoxes != null && !detectionBoxes.isEmpty()) {
            for (AIDetectionResult.DetectionBox box : detectionBoxes) {
                drawSingleBox(g2d, box, label);
            }
        }
        // 释放资源
        g2d.dispose();

        return image;
    }
    /**
     * 绘制单个检测框和标签
     * @param g2d 绘图上下文
     * @param box 检测框信息
     * @param label 标签文本
     */
    private void drawSingleBox(Graphics2D g2d, AIDetectionResult.DetectionBox box, String label) {
        // 转换为整数坐标（因为绘图API需要整数坐标）
        int x1 = box.getX1().intValue();
        int y1 = box.getY1().intValue();
        int x2 = box.getX2().intValue();
        int y2 = box.getY2().intValue();
        // 绘制矩形检测框
        g2d.drawRect(x1, y1, x2 - x1, y2 - y1);
        // 准备标签文本（包含标签和置信度）
        String displayText = String.format("%s (%.2f)", label, box.getConfidence());
        // 获取字体信息，用于计算文本尺寸
        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(displayText);
        int textHeight = metrics.getHeight();
        // 计算标签背景位置（位于检测框上方）
        int bgX = x1;
        int bgY = y1 - textHeight;
        // 确保标签不会超出图像顶部
        if (bgY < 0) {
            bgY = y1;
        }
        // 绘制标签背景（半透明黑色矩形）
        g2d.setColor(new Color(0, 0, 0, 180)); // 180是透明度（0-255）
        g2d.fillRect(bgX, bgY, textWidth + 8, textHeight);
        // 绘制标签文本（白色）
        g2d.setColor(Color.WHITE);
        g2d.drawString(displayText, bgX + 4, bgY + metrics.getAscent());
    }
}

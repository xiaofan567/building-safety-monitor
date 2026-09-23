package com.qst.smartbuildings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置类 - 固定路径映射（写死配置）。
 *
 * <p>负责将本地存储目录映射为静态资源访问路径，供前端直接下载/预览同步到本地的 Coze 报告文档。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Coze 同步文档的本地存储路径（相对或绝对） */
    @Value("${coze.document.storage-path}")
    private String documentStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 固定映射规则：将/ai_image/** 映射到项目根目录同级的ai_image文件夹
        // 1. 获取项目根目录（与src同级）
        String projectRoot = System.getProperty("user.dir");

        // 2. 拼接ai_image目录的绝对路径（写死路径规则）
        String aiImageAbsolutePath = projectRoot + File.separator + "ai_image" + File.separator;

        // 3. 注册资源处理器
        registry.addResourceHandler("/ai_image/**")  // 访问路径
                .addResourceLocations("file:" + aiImageAbsolutePath)  // 本地文件路径
                .setCachePeriod(0);  // 开发环境不缓存，生产环境可改为3600等

        // 4. 将 /doc-files/** 映射到文档同步存储目录
        // documentStoragePath 是相对路径（如 ./doc-files/），需拼接项目根目录
        File docFilesDir = new File(documentStoragePath);
        if (!docFilesDir.isAbsolute()) {
            docFilesDir = new File(projectRoot, documentStoragePath);
        }
        String docFilesPath = docFilesDir.getAbsolutePath();
        if (!docFilesPath.endsWith(File.separator)) {
            docFilesPath += File.separator;
        }
        registry.addResourceHandler("/doc-files/**")
                .addResourceLocations("file:" + docFilesPath)
                .setCachePeriod(3600);
    }
}
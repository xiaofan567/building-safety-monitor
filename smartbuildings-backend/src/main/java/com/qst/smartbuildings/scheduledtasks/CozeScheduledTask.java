package com.qst.smartbuildings.scheduledtasks;

import com.qst.smartbuildings.service.CozeChatService;
import com.qst.smartbuildings.service.CozeDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Coze 定时任务。
 *
 * <p>由 application.yml 中的 {@code coze.cron} 驱动触发，默认每天 20:00 执行。
 * 任务流程：
 * <ol>
 *     <li>调用 {@link CozeChatService#executeAndRecord()} 生成日报数据并推送 Coze 智能体；</li>
 *     <li>执行成功后延迟 5 分钟异步触发文档同步（Coze 数据处理约需 3 分钟，留出缓冲）。</li>
 * </ol>
 * 可通过 {@code coze.scheduler.enabled=false} 关闭定时任务。</p>
 */
@Component
public class CozeScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(CozeScheduledTask.class);

    @Autowired
    private CozeChatService cozeChatService;

    @Autowired
    private CozeDatabaseService cozeDatabaseService;

    @Value("${coze.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final ScheduledExecutorService delayExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "coze-sync-delay");
        t.setDaemon(true);
        return t;
    });

    /**
     * 核心执行逻辑 — 供 @Scheduled 注解和手动触发复用。
     */
    public void execute() {
        log.info("Coze 定时任务开始执行");
        boolean success = false;
        try {
            success = cozeChatService.executeAndRecord();
        } catch (Exception e) {
            log.error("Coze 定时任务执行异常: {}", e.getMessage(), e);
        }
        log.info("Coze 定时任务执行{}", success ? "成功" : "失败");

        // 执行成功后，延迟 5 分钟自动同步文档（Coze 数据处理约需 3 分钟）
        if (success) {
            delayExecutor.schedule(() -> {
                try {
                    log.info("Coze 定时任务: 报告生成成功，开始延迟同步文档");
                    String result = cozeDatabaseService.syncDocuments();
                    log.info("Coze 定时任务: 延迟同步文档完成: {}", result);
                } catch (Exception e) {
                    log.error("Coze 定时任务: 延迟同步文档失败: {}", e.getMessage(), e);
                }
            }, 5, TimeUnit.MINUTES);
        }
    }

    /**
     * 注解方式的定时调用（由 application.yml 中的 coze.cron 控制）。
     * 通过 coze.scheduler.enabled 配置控制是否实际执行。
     */
    @Scheduled(cron = "${coze.cron}")
    public void scheduledExecute() {
        if (!schedulerEnabled) {
            log.debug("Coze 定时任务已禁用 (coze.scheduler.enabled=false)，跳过执行");
            return;
        }
        execute();
    }
}
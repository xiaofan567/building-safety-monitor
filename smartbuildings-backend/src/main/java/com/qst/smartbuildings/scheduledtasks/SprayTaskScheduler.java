package com.qst.smartbuildings.scheduledtasks;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.smartbuildings.common.EquipmentTypeConst;
import com.qst.smartbuildings.common.RedisConst;
import com.qst.smartbuildings.entity.EquipmentAssets;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.environment.SprayTask;
import com.qst.smartbuildings.mapper.EquipmentAssetsMapper;
import com.qst.smartbuildings.mapper.environment.SpraySystemMapper;
import com.qst.smartbuildings.mapper.environment.SprayTaskMapper;
import com.qst.smartbuildings.tcpserver.TCPClientPool;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 喷淋任务调度器：每30分钟扫描一次，对即将执行的任务设置精确调度。
 */
@Slf4j
@Component
public class SprayTaskScheduler {

    private static final long SCAN_INTERVAL_MS = 30 * 60 * 1000L;
    private static final int OPEN = 1;
    private static final int CLOSE = 0;
    private static final int FAILED = 2;

    private final SprayTaskMapper sprayTaskMapper;
    private final TCPClientPool tcpClientPool;
    private final RedisClient redisClient;
    private final EquipmentAssetsMapper equipmentAssetsMapper;
    private final SpraySystemMapper spraySystemMapper;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    public SprayTaskScheduler(SprayTaskMapper sprayTaskMapper,
                              TCPClientPool tcpClientPool,
                              RedisClient redisClient,
                              EquipmentAssetsMapper equipmentAssetsMapper,
                              SpraySystemMapper spraySystemMapper) {
        this.sprayTaskMapper = sprayTaskMapper;
        this.tcpClientPool = tcpClientPool;
        this.redisClient = redisClient;
        this.equipmentAssetsMapper = equipmentAssetsMapper;
        this.spraySystemMapper = spraySystemMapper;
    }

    @PostConstruct
    public void init() {
        taskScheduler.setPoolSize(4);
        taskScheduler.setThreadNamePrefix("spray-task-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.initialize();
    }

    @PreDestroy
    public void destroy() {
        taskScheduler.shutdown();
    }

    /**
     * 每30分钟扫描启用任务，并为扫描窗口内的任务创建精确执行任务。
     */
    @Scheduled(fixedRate = SCAN_INTERVAL_MS)
    public void scanAndScheduleTasks() {
        Date now = new Date();
        Date windowEnd = new Date(now.getTime() + SCAN_INTERVAL_MS);
        List<SprayTask> tasks = sprayTaskMapper.selectList(
                new LambdaQueryWrapper<SprayTask>()
                        .eq(SprayTask::getIsEnable, "1")
                        .and(wrapper -> wrapper.isNull(SprayTask::getNextTime)
                                .or()
                                .le(SprayTask::getNextTime, windowEnd)));
        log.info("开始加载喷淋任务，扫描到{}条任务", tasks.size());
        for (SprayTask task : tasks) {
            try {
                scheduleTaskExecution(task, now, windowEnd);
            } catch (Exception e) {
                log.error("加载喷淋任务失败，任务id={}", task.getId(), e);
            }
        }
    }

    private void scheduleTaskExecution(SprayTask task, Date now, Date windowEnd) {
        if (!isTaskValid(task)) {
            return;
        }

        Date nextTime = task.getNextTime();
        if (nextTime == null || !nextTime.after(now)) {
            nextTime = calculateNextExecutionTime(task.getStartTime(), now,
                    task.getCycle(), task.getCycleUnit());
            if (nextTime.after(windowEnd)) {
                updateNextTime(task, nextTime);
                return;
            }
        }

        Date executionTime = nextTime;
        Date followingTime = calculateNextExecutionTime(task.getStartTime(), executionTime,
                task.getCycle(), task.getCycleUnit());
        scheduleStart(task, executionTime);
        scheduleStop(task, executionTime);
        updateNextTime(task, followingTime);
        log.info("喷淋任务已调度，任务id={}，开始时间={}，结束时间={}，下次执行时间={}",
                task.getId(), executionTime,
                new Date(executionTime.getTime() + task.getDuration() * 60 * 1000L), followingTime);
    }

    private boolean isTaskValid(SprayTask task) {
        boolean valid = task.getStartTime() != null
                && task.getLocationId() != null
                && task.getDuration() != null
                && task.getDuration() > 0
                && task.getCycle() != null
                && task.getCycle() > 0
                && task.getCycleUnit() != null;
        if (!valid) {
            log.warn("喷淋任务配置不完整，跳过调度，任务id={}", task.getId());
        }
        return valid;
    }

    private void scheduleStart(SprayTask task, Date executionTime) {
        String key = buildScheduleKey(task.getId(), executionTime, "start");
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeSprinklerTask(task, OPEN, key), executionTime);
        if (future != null) {
            scheduledTasks.put(key, future);
        }
    }

    private void scheduleStop(SprayTask task, Date executionTime) {
        Date endTime = new Date(executionTime.getTime() + task.getDuration() * 60 * 1000L);
        String key = buildScheduleKey(task.getId(), executionTime, "stop");
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeSprinklerTask(task, CLOSE, key), endTime);
        if (future != null) {
            scheduledTasks.put(key, future);
        }
    }

    private void executeSprinklerTask(SprayTask task, Integer status, String scheduleKey) {
        try {
            List<EquipmentAssets> sprinklerList = equipmentAssetsMapper.selectList(
                    new LambdaQueryWrapper<EquipmentAssets>()
                            .eq(EquipmentAssets::getLocationId, task.getLocationId())
                            .eq(EquipmentAssets::getTypeId, EquipmentTypeConst.SPRINKLER_TYPE_ID)
                            .eq(EquipmentAssets::getOpenStatus, "1"));
            log.info("{}喷淋设备，位置id为{}，喷淋设备数量为{}",
                    status == OPEN ? "开启" : "关闭", task.getLocationId(), sprinklerList.size());
            for (EquipmentAssets sprinkler : sprinklerList) {
                sendSprinkler(task, sprinkler, status);
            }
        } finally {
            scheduledTasks.remove(scheduleKey);
        }
    }

    private void sendSprinkler(SprayTask task, EquipmentAssets sprinkler, Integer status) {
        String clientId = getClientId(sprinkler);
        boolean success = clientId != null
                && tcpClientPool.sendMessage(clientId, buildControlMessage(status));
        if (!success) {
            log.error("喷淋设备{}的TCP连接不存在或发送失败", sprinkler.getAssetName());
        }
        spraySystemMapper.insert(buildSpraySystem(task, sprinkler,
                success ? status : FAILED));
    }

    private String getClientId(EquipmentAssets sprinkler) {
        if (sprinkler.getPlatformEqCode() == null) {
            return null;
        }
        Object clientId = redisClient.get(RedisConst.ASSETS_NAME_HEAD + sprinkler.getPlatformEqCode());
        return clientId == null ? null : clientId.toString();
    }

    private String buildControlMessage(Integer status) {
        return String.format("{status:control, message:%s, timestamp:%d}",
                status == OPEN ? "open" : "close", System.currentTimeMillis());
    }

    private SpraySystem buildSpraySystem(SprayTask task,
                                         EquipmentAssets sprinkler,
                                         Integer status) {
        SpraySystem spraySystem = new SpraySystem();
        spraySystem.setEquipmentId(sprinkler.getAssetId().intValue());
        spraySystem.setLocationId(sprinkler.getLocationId().intValue());
        spraySystem.setTriggerType(2);
        spraySystem.setOperationType(status.toString());
        spraySystem.setOperationReason(buildOperationReason(task, sprinkler, status));
        spraySystem.setOperationTime(LocalDateTime.now());
        return spraySystem;
    }

    private String buildOperationReason(SprayTask task,
                                        EquipmentAssets sprinkler,
                                        Integer status) {
        if (status == FAILED) {
            return String.format("喷淋执行失败，任务ID：%s，设备ID：%s，设备名：%s，原因：设备离线或TCP连接不可用",
                    task.getId(), sprinkler.getAssetId(), sprinkler.getAssetName());
        }
        return String.format("喷淋任务%s执行，任务ID：%s，设备ID：%s，%s喷淋操作",
                task.getTaskName(), task.getId(), sprinkler.getAssetId(),
                status == OPEN ? "开启" : "关闭");
    }

    private void updateNextTime(SprayTask task, Date nextTime) {
        task.setNextTime(nextTime);
        sprayTaskMapper.updateById(task);
    }

    private String buildScheduleKey(Integer taskId, Date executionTime, String action) {
        return taskId + "_" + executionTime.getTime() + "_" + action;
    }

    /**
     * 计算严格晚于当前时间的下一次执行时间。
     */
    public static Date calculateNextExecutionTime(Date startTime,
                                                  Date currentTime,
                                                  long interval,
                                                  String timeUnit) {
        if (startTime == null || currentTime == null) {
            throw new IllegalArgumentException("Start time and current time must not be null");
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than zero");
        }
        if (startTime.after(currentTime)) {
            return new Date(startTime.getTime());
        }

        String unit = timeUnit == null ? "" : timeUnit.trim().toUpperCase(Locale.ROOT);
        switch (unit) {
            case "MINUTES":
                return fixedIntervalTime(startTime, currentTime, interval, 60 * 1000L);
            case "HOURS":
                return fixedIntervalTime(startTime, currentTime, interval, 60 * 60 * 1000L);
            case "DAYS":
                return fixedIntervalTime(startTime, currentTime, interval, 24 * 60 * 60 * 1000L);
            case "WEEKS":
                return calendarBasedTime(startTime, currentTime, (int) interval, Calendar.WEEK_OF_YEAR);
            case "MONTHS":
                return calendarBasedTime(startTime, currentTime, (int) interval, Calendar.MONTH);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + timeUnit);
        }
    }

    private static Date fixedIntervalTime(Date startTime,
                                          Date currentTime,
                                          long interval,
                                          long unitMillis) {
        long start = startTime.getTime();
        long intervalMillis = interval * unitMillis;
        long elapsed = (currentTime.getTime() - start) / intervalMillis;
        return new Date(start + (elapsed + 1) * intervalMillis);
    }

    private static Date calendarBasedTime(Date startTime,
                                          Date currentTime,
                                          int interval,
                                          int calendarField) {
        Calendar next = Calendar.getInstance();
        next.setTime(startTime);
        while (!next.after(currentTime)) {
            next.add(calendarField, interval);
        }
        return next.getTime();
    }
}

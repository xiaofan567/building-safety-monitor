package com.qst.smartbuildings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 安全日报数据传输对象。
 *
 * <p>封装日报生成所需的全部业务数据，作为 Coze 智能体输入的 JSON 结构。
 * 与 coze4.md 任务定义的字段保持一致。</p>
 */
@Data
@Accessors(chain = true)
@Schema(description = "安全日报数据传输对象")
public class SecurityDailyReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报告生成时间")
    private String reportDate;

    @Schema(description = "报告统计周期")
    private String reportPeriod;

    @Schema(description = "报告统计起始时间")
    private String startDate;

    @Schema(description = "报告统计截止时间")
    private String endDate;

    @Schema(description = "危大工程设备总览")
    private Overview overview;

    @Schema(description = "塔吊实时数据及报警记录")
    private TowerCraneSection towerCrane;

    @Schema(description = "施工升降机实时数据及报警记录")
    private ElevatorSection elevator;

    @Schema(description = "高支模监测点数据")
    private FormworkSection formwork;

    @Schema(description = "深基坑监测点数据")
    private FoundationPitSection foundationPit;

    @Schema(description = "环境监测数据")
    private EnvironmentSection environment;

    @Schema(description = "AI 视频报警记录")
    private AiAlertsSection aiAlerts;

    @Schema(description = "设备监测报警记录")
    private MonitorAlertsSection monitorAlerts;

    @Schema(description = "设备上下线记录")
    private OfflineRecordsSection offlineRecords;

    // ======== Overview ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "危大工程设备总览")
    public static class Overview implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "各类型设备统计")
        private List<TypeStat> typeStats;

        @Schema(description = "整体在线率(%)")
        private Double overallOnlineRate;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "设备类型统计")
    public static class TypeStat implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "类型名称")
        private String typeName;

        @Schema(description = "类型ID")
        private Integer typeId;

        @Schema(description = "总数")
        private Integer total;

        @Schema(description = "在线数")
        private Integer online;

        @Schema(description = "昨日报警数")
        private Integer alarmYesterday;

        @Schema(description = "今日报警数")
        private Integer alarmToday;

        @Schema(description = "昨日预警数")
        private Integer warnYesterday;

        @Schema(description = "今日预警数")
        private Integer warnToday;
    }

    // ======== Tower Crane ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "塔吊章节")
    public static class TowerCraneSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<TowerDevice> devices;
        private List<Object> alarms;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "塔吊设备数据")
    public static class TowerDevice implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long assetId;
        private String deviceName;
        private Float weight;
        private Float moment;
        private Float amplitude;
        private Float rotation;
        private Float windSpeed;
        private Float height;
    }

    // ======== Elevator ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "施工升降机章节")
    public static class ElevatorSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<ElevatorDevice> devices;
        private List<Object> alarms;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "升降机设备数据")
    public static class ElevatorDevice implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long assetId;
        private String deviceName;
        private Double weight;
        private Double weightPercent;
        private Double height;
        private Double xAngle;
        private Double yAngle;
        private Double bias;
        private Double wind;
        private Double speed;
    }

    // ======== Formwork (高支模) ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "高支模章节")
    public static class FormworkSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<FormworkDevice> devices;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "高支模设备")
    public static class FormworkDevice implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long assetId;
        private String deviceName;
        private Map<String, Object> monitorPoints;
    }

    // ======== Foundation Pit (深基坑) ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "深基坑章节")
    public static class FoundationPitSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<FoundationPitDevice> devices;
        private List<Object> alarms;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "深基坑设备")
    public static class FoundationPitDevice implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long assetId;
        private String deviceName;
        private Map<String, Object> monitorPoints;
    }

    // ======== Environment ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "环境监测章节")
    public static class EnvironmentSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private Map<String, Object> envInfo;
        private List<Object> sprayRecords;
    }

    // ======== AI Alerts ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "AI 识别报警章节")
    public static class AiAlertsSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Object> records;
        private Map<String, Integer> typeStats;
    }

    // ======== Monitor Alerts ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "设备监测报警章节")
    public static class MonitorAlertsSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Object> records;
        private Map<String, Integer> alertLevelStats;
    }

    // ======== Offline Records ========
    @Data
    @Accessors(chain = true)
    @Schema(description = "离线记录章节")
    public static class OfflineRecordsSection implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Object> records;
    }
}
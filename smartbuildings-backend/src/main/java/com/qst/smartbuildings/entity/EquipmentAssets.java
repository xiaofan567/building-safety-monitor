package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备资产信息实体
 *
 * @author 智能建造团队
 */
@Data
@TableName("equipment_assets")
@Schema(description = "设备资产信息实体")
public class EquipmentAssets implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "asset_id", type = IdType.AUTO)
    @Schema(description = "资产ID")
    private Long assetId;

    @Schema(description = "资产编号")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "资产位置")
    private Long locationId;

    @Schema(description = "设备类型")
    private Long typeId;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "状态 字典项 1：正常 0：报废")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "重要程度 字典项")
    private String level;

    @Schema(description = "生产厂家")
    private String factory;

    @Schema(description = "出厂日期")
    private LocalDate productionDate;

    @Schema(description = "供货商")
    private String supplier;

    @Schema(description = "供货日期")
    private LocalDate supplyDate;

    @Schema(description = "验收日期")
    private LocalDate acceptanceDate;

    @Schema(description = "安装日期")
    private LocalDate installDate;

    @Schema(description = "启动日期")
    private LocalDate startDate;

    @Schema(description = "报废预期")
    private LocalDate scrapPre;

    @Schema(description = "保修期限")
    private Integer serviceTerm;

    @Schema(description = "报废日期")
    private LocalDate scrapDate;

    @Schema(description = "保修截止日期")
    private LocalDate serviceDate;

    @Schema(description = "设备原值")
    private Integer original;

    @Schema(description = "评估价值")
    private Integer originalPre;

    @Schema(description = "资产照片")
    private String assetPic;

    @Schema(description = "备注说明")
    private String remarks;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateUserId;

    @Schema(description = "对接平台")
    private Integer platform;

    @Schema(description = "相对位置大屏")
    private String planeCoordinate;

    @Schema(description = "相对位置pc")
    private String planeCoordinatePc;

    @Schema(description = "平台设备标识")
    private String platformEqCode;

    @Schema(description = "上下线时间")
    private LocalDateTime offlineDate;

    @Schema(description = "状态：1在线，0离线")
    private String offlineStatus;

    @Schema(description = "开启状态")
    private String openStatus;

    @TableField(exist = false)
    @Schema(description = "位置名称（联表冗余，非表字段）")
    private String locationName;

    @TableField(exist = false)
    @Schema(description = "设备类型名称（联表冗余，非表字段）")
    private String typeName;

    @TableField(exist = false)
    @Schema(description = "扩展参数（JSON字符串，用于危大工程设备参数初始化，如塔机额定力矩等）")
    @JsonDeserialize(using = EquipmentAssets.StringOrObjectDeserializer.class)
    private String extraParams;

    /**
     * 扩展参数反序列化器：兼容前端/测试以 JSON 对象或 JSON 字符串两种形式传递。
     * 字段实际类型为 String，当请求体传的是对象时，将其序列化为 JSON 字符串存储。
     */
    public static class StringOrObjectDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isTextual() || node.isNull()) {
                return node.isNull() ? null : node.textValue();
            }
            return node.toString();
        }
    }
}

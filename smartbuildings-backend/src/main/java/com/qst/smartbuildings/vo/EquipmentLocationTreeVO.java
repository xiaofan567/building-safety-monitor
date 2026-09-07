package com.qst.smartbuildings.vo;

import com.qst.smartbuildings.entity.EquipmentLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备位置树形结构 VO
 *
 * @author 智能建造团队
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "设备位置树形结构 VO")
public class EquipmentLocationTreeVO extends EquipmentLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "子节点列表")
    private List<EquipmentLocationTreeVO> children = new ArrayList<>();
}
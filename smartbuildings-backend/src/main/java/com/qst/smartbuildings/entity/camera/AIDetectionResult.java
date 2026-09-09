package com.qst.smartbuildings.entity.camera;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "统一检测结果实体")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIDetectionResult {

    @Schema(description = "检测类型（1=安全帽，2=安全衣，3=吸烟，6=明火）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @JsonProperty("type")
    private Integer type;

    @Schema(description = "总检测目标数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @JsonProperty("total_objects")
    private Integer totalObjects;

    @Schema(description = "有效违规数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @JsonProperty("valid_count")
    private Integer validCount;

    @Schema(description = "违规目标坐标及置信度列表")
    @JsonProperty("boxes")
    private List<DetectionBox> boxes;
    public String getTypeName(){
        switch (type){
            case 1:
                return "未带安全帽";
            case 2:
                return "未穿安全衣";
            case 3:
                return "工作现场吸烟";
            case 4:
                return "夜间施工";
            case 5:
                return "工作现场明烟";
            case 6:
                return "工作现场明火";
            default:
                return "未知";
        }
    }


    /**
     * 坐标及置信度信息
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @Schema(description = "坐标及置信度信息")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DetectionBox {

        @Schema(description = "左上角X坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.5")
        @JsonProperty("x1")
        private Double x1;

        @Schema(description = "左上角Y坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "200.3")
        @JsonProperty("y1")
        private Double y1;

        @Schema(description = "右下角X坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.7")
        @JsonProperty("x2")
        private Double x2;

        @Schema(description = "右下角Y坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "250.2")
        @JsonProperty("y2")
        private Double y2;

        @Schema(description = "置信度（范围0-1）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.85")
        @JsonProperty("confidence")
        private Double confidence;

        // ====== Getter/Setter ======
        public Double getX1() { return x1; }
        public void setX1(Double x1) { this.x1 = x1; }

        public Double getY1() { return y1; }
        public void setY1(Double y1) { this.y1 = y1; }

        public Double getX2() { return x2; }
        public void setX2(Double x2) { this.x2 = x2; }

        public Double getY2() { return y2; }
        public void setY2(Double y2) { this.y2 = y2; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
    /*
     * 检查检测框是否有变化
     * @param oldList 旧的检测框列表
     * @param newList 新的检测框列表
     * @return 是否变化类别
     */
    public static Boolean checkDetectionResult(List<AIDetectionResult>oldList,List<AIDetectionResult>newList){
        //一般新列表都是有报警信息的，如果旧列表为空，则表示需要报警
        if(oldList==null||oldList.size()==0){
            return true;
        }
        //两个列表不相等，则表示检测目标不一致，需要进行报警
        if(oldList.size()!=newList.size()){
            return true;
        }
        //快速鉴别两个列表是否相同的方法，先将旧列表存到Map中，然后新列表根据type取一遍，总目标数或有效目标数不一致，就是不同
        HashMap<Integer, AIDetectionResult> integerAIDetectionResultHashMap = new HashMap<>();
        for(AIDetectionResult result:oldList){
            integerAIDetectionResultHashMap.put(result.getType(),result);
        }
        for(AIDetectionResult result:newList){
            AIDetectionResult oldResult = integerAIDetectionResultHashMap.get(result.getType());
            if(oldResult==null){
                return true;
            }
            if(oldResult.getValidCount()<result.getValidCount()){
                return true;
            }
        }
        return false;
    }
    /*
     * 检测是否有有效的目标值
     * @param list 检测框列表
     * @return 是否有有效的目标值
     */
    public static Boolean isValidDetectionResult(List<AIDetectionResult>list){
        if(list == null || list.isEmpty()){
            return false;
        }
        for(AIDetectionResult result:list){
            if(result.getValidCount() > 0){
                return true;
            }
        }
        return false;
    }
}

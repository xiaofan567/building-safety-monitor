package com.qst.smartbuildings.entity.coze;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Coze 知识库文档本地镜像。
 *
 * <p>用于保存 Coze 知识库（safe_report_list 数据库）同步到本地的文档元数据与本地文件路径，
 * 提供毫秒级查询能力并避免对 Coze 平台签名 URL 的长期依赖。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("coze_document")
@Schema(name = "CozeDocument", description = "Coze 知识库文档本地镜像表")
public class CozeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "Coze 文档唯一标识（优先使用记录 id，缺失时回退 doc_id）")
    private String docId;

    @Schema(description = "文档名称")
    private String docName;

    @Schema(description = "本地存储的相对路径（基于 coze.document.storage-path）")
    private String docUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "Coze 平台上的文档创建时间")
    private Date bstudioCreateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "本地记录创建时间")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "本地记录更新时间")
    private Date updatedAt;
}
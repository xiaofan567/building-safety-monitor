package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.smartbuildings.entity.coze.CozeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * Coze 知识库文档本地镜像数据操作接口。
 *
 * <p>无对应 XML 文件，继承 MyBatis-Plus {@link BaseMapper} 提供基础 CRUD。</p>
 */
@Mapper
public interface CozeDocumentMapper extends BaseMapper<CozeDocument> {
}
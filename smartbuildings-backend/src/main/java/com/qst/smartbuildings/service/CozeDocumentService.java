package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qst.smartbuildings.entity.coze.CozeDocument;

import java.util.List;

/**
 * Coze 知识库文档本地镜像服务接口。
 */
public interface CozeDocumentService extends IService<CozeDocument> {

    /**
     * 根据 docId 查询单条记录。
     *
     * @param docId Coze 文档唯一标识
     * @return 本地镜像记录，无则返回 null
     */
    CozeDocument getByDocId(String docId);

    /**
     * 获取本地已同步的所有 docId 列表（用于增量同步去重判断）。
     *
     * @return docId 集合
     */
    List<String> listAllDocIds();

    /**
     * 按文档名称与日期范围分页查询本地文档。
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @param docName  文档名称模糊匹配（可空）
     * @param startDate 开始日期，格式 yyyy-MM-dd（可空）
     * @param endDate  结束日期，格式 yyyy-MM-dd（可空）
     * @return 分页结果
     */
    IPage<CozeDocument> selectDocumentList(int pageNum, int pageSize,
                                           String docName, String startDate, String endDate);
}
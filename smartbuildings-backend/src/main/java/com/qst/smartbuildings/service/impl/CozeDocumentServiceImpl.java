package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.coze.CozeDocument;
import com.qst.smartbuildings.mapper.CozeDocumentMapper;
import com.qst.smartbuildings.service.CozeDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Coze 知识库文档本地镜像服务实现。
 */
@Service
public class CozeDocumentServiceImpl extends ServiceImpl<CozeDocumentMapper, CozeDocument>
        implements CozeDocumentService {

    @Override
    public CozeDocument getByDocId(String docId) {
        LambdaQueryWrapper<CozeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CozeDocument::getDocId, docId);
        return getOne(wrapper, false);
    }

    @Override
    public List<String> listAllDocIds() {
        LambdaQueryWrapper<CozeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CozeDocument::getDocId);
        return list(wrapper).stream()
                .map(CozeDocument::getDocId)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<CozeDocument> selectDocumentList(int pageNum, int pageSize,
                                                   String docName, String startDate, String endDate) {
        Page<CozeDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CozeDocument> wrapper = new LambdaQueryWrapper<>();
        if (docName != null && !docName.isEmpty()) {
            wrapper.like(CozeDocument::getDocName, docName);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(CozeDocument::getBstudioCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(CozeDocument::getBstudioCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(CozeDocument::getBstudioCreateTime);
        return page(page, wrapper);
    }
}
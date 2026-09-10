package com.qst.smartbuildings.service.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.environment.SprayTask;
import com.qst.smartbuildings.entity.param.SprayTaskParam;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 喷淋任务服务接口
 *
 * @author system
 * @since 1.0
 */
public interface SprayTaskService extends IService<SprayTask> {

    /**
     * 分页查询喷淋任务
     *
     * @param sprayTaskParam 查询条件
     * @return 分页结果
     */
    BPage<SprayTask> selectTaskByPage(SprayTaskParam sprayTaskParam);
}

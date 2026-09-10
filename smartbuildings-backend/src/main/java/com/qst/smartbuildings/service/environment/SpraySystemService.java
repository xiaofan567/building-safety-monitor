package com.qst.smartbuildings.service.environment;

import com.qst.smartbuildings.entity.BPage;
import com.qst.smartbuildings.entity.SpraySystem;
import com.qst.smartbuildings.entity.param.SpraySystemParam;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 喷淋系统服务接口
 *
 * @author system
 * @since 1.0
 */
public interface SpraySystemService extends IService<SpraySystem> {

    /**
     * 分页查询喷淋系统记录
     *
     * @param spraySystemParam 查询条件
     * @return 分页结果
     */
    BPage<SpraySystem> selectByPage(SpraySystemParam spraySystemParam);
}

package com.qst.smartbuildings.service;

import com.qst.smartbuildings.vo.SecurityDailyReportVo;

/**
 * 安全日报数据组装服务接口。
 *
 * <p>聚合多源业务数据（设备总览、塔吊、升降机、高支模、深基坑、环境、AI 报警、设备报警、离线记录），
 * 生成用于 Coze 智能体处理的完整日报数据结构。</p>
 */
public interface SecurityDailyReportService {

    /**
     * 获取当日完整的安全日报数据。
     *
     * @return 包含报告周期、各模块数据的安全日报 VO
     */
    SecurityDailyReportVo getDailyReport();
}
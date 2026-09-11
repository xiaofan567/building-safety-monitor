package com.qst.smartbuildings.tcpserver.handle;

import com.qst.smartbuildings.common.UpdateOperationEnum;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备数据处理通用抽象类
 *
 * <p>为各类设备（塔机、升降机等）的数据处理提供统一的内存缓存结构（以平台设备标识 platformEqCode 为 Key），
 * 并定义数据初始化、数据下发处理与设备信息增删改的内存同步方法。</p>
 *
 * @param <T> 设备信息实体类型
 * @author 智能建造团队
 */
public abstract class DeviceDataProcessHandle<T> {

    /**
     * 设备信息内存缓存，以平台设备标识 platformEqCode 为 Key（ConcurrentHashMap 保证多线程安全）
     */
    protected final Map<String, T> dataMap = new ConcurrentHashMap<>();

    /**
     * 初始化设备信息到内存缓存
     */
    public abstract void init();

    /**
     * 处理设备上报的数据
     *
     * @param data     设备上报数据
     * @param clientId TCP客户端标识
     */
    public abstract void processData(DeviceReportDTO data, String clientId);

    /**
     * 同步设备信息变更到内存缓存（新增、修改、删除）
     *
     * @param data 设备信息
     * @param key  平台设备标识
     * @param type 操作类型（add：新增，update：修改，delete：删除）
     */
    public void update(T data, String key, UpdateOperationEnum type) {
        // 无平台设备标识的设备不参与 TCP 内存缓存同步（ConcurrentHashMap 不允许 null key）
        if (key == null || key.isEmpty()) {
            return;
        }
        if (type == UpdateOperationEnum.DELETE) {
            dataMap.remove(key);
        } else {
            dataMap.put(key, data);
        }
    }
}
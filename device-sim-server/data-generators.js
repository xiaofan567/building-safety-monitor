/**
 * 数据生成器 - 根据设备配置生成模拟数据
 *
 * 与设备通信模拟服务端（server.js）配合使用：
 *  - 每个设备类型对应一个生成函数，返回 { code, id, properties } 格式
 *  - 参数值在配置的 min/max 范围内随机生成
 *  - 作业数据（塔吊作业/升降机作业）额外包含 startTime/endTime
 *
 * 事件约定：HTTP 接口 GET /api/data/:deviceId 与 Socket.IO data_sent 事件共用本生成器。
 */

// 随机数生成器
function getRandomInRange(min, max, fixed = 2) {
  return Number((Math.random() * (max - min) + min).toFixed(fixed))
}

// 时间生成器：返回 'YYYY-MM-DD HH:mm:ss' 格式
function getCurrentTime() {
  return new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
}

// 生成作业数据的结束时间：开始时间后 5~15 分钟内
function getWorkEndTime(startMs = Date.now()) {
  const minutes = 5 + Math.floor(Math.random() * 11) // 5 ~ 15
  return new Date(startMs + minutes * 60 * 1000).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).replace(/\//g, '-')
}

// 塔吊作业数据：记录一次完整吊装作业过程
function generateTowerCraneWorkData(config) {
  const startMs = Date.now()
  return {
    code: config.code,
    id: config.id,
    properties: {
      liftingPointHeight: getRandomInRange(config.liftingPointHeight.min, config.liftingPointHeight.max),           // 起吊高度（米）
      liftingPointHeightUnload: getRandomInRange(config.liftingPointHeightUnload.min, config.liftingPointHeightUnload.max), // 卸吊高度（米）
      liftingPointRadius: getRandomInRange(config.liftingPointRadius.min, config.liftingPointRadius.max),           // 起吊半径（米）
      liftingPointRadiusUnload: getRandomInRange(config.liftingPointRadiusUnload.min, config.liftingPointRadiusUnload.max), // 卸吊半径（米）
      startAngle: getRandomInRange(config.startAngle.min, config.startAngle.max),         // 起吊角度（度）
      endAngle: getRandomInRange(config.endAngle.min, config.endAngle.max),               // 卸吊角度（度）
      maxPercent: getRandomInRange(config.maxPercent.min, config.maxPercent.max),         // 最大力矩百分比（%）
      maxWindSpeed: getRandomInRange(config.maxWindSpeed.min, config.maxWindSpeed.max),   // 最大风速（m/s）
      weight: getRandomInRange(config.weight.min, config.weight.max),                     // 重量（kg）
      minRadius: getRandomInRange(config.minRadius.min, config.minRadius.max),            // 最小半径（米）
      maxRadius: getRandomInRange(config.maxRadius.min, config.maxRadius.max),            // 最大半径（米）
      minHeight: getRandomInRange(config.minHeight.min, config.minHeight.max),            // 最小高度（米）
      maxHeight: getRandomInRange(config.maxHeight.min, config.maxHeight.max),            // 最大高度（米）
      startTime: getCurrentTime(),                                                        // 开始时间
      endTime: getWorkEndTime(startMs)                                                    // 结束时间（5~15 分钟后）
    }
  }
}

// 塔吊实时数据：塔吊当前运行状态的实时监测数据
function generateTowerCraneRealTimeData(config) {
  return {
    code: config.code,
    id: config.id,
    properties: {
      monitorTime: getCurrentTime(),                                                                     // 监测时间
      rotation: getRandomInRange(config.rotation.min, config.rotation.max),                               // 回转角度（度）
      heightAlarm: '0',                                                                                  // 高度报警（0-正常）
      weight: getRandomInRange(config.weight.min, config.weight.max),                                    // 当前重量（吨）
      momentAlarm: '0',                                                                                  // 力矩报警（0-正常）
      windSpeedAlarm: '0',                                                                               // 风速报警（0-正常）
      equipmentId: config.id ?? 1001,                                                                    // 设备ID
      noAlarm: '0',                                                                                      // 无报警状态
      weightAlarm: Math.random() > 0.8 ? '2' : '0',                                                      // 重量报警（0-正常，2-超载）
      moment: getRandomInRange(config.moment.min, config.moment.max, 3),                                  // 当前力矩（kN·m）
      amplitude: getRandomInRange(config.amplitude.min, config.amplitude.max),                            // 当前幅度（米）
      windSpeed: getRandomInRange(config.windSpeed.min, config.windSpeed.max),                            // 当前风速（m/s）
      obliquity: getRandomInRange(config.obliquity.min, config.obliquity.max),                           // 倾斜角度（度）
      height: getRandomInRange(config.height.min, config.height.max)                                     // 当前高度（米）
    }
  }
}

// 升降机作业数据：一次完整升降作业过程的数据记录
function generateElevatorWorkData(config) {
  const startMs = Date.now()
  return {
    code: config.code,
    id: config.id,
    properties: {
      angleY: getRandomInRange(config.angleY.min, config.angleY.max),                                     // Y轴倾斜角度（度）
      endFloor: Math.floor(getRandomInRange(config.endFloor.min, config.endFloor.max)),                   // 结束楼层
      weight: getRandomInRange(config.weight.min, config.weight.max),                                     // 载重（吨）
      speed: getRandomInRange(config.speed.min, config.speed.max),                                        // 运行速度（m/s）
      startHeight: getRandomInRange(config.startHeight.min, config.startHeight.max),                      // 起始高度（米）
      endHeight: getRandomInRange(config.endHeight.min, config.endHeight.max),                            // 结束高度（米）
      createTime: getCurrentTime(),                                                                       // 创建时间
      angle: getRandomInRange(config.angle.min, config.angle.max),                                        // 倾斜角度（度）
      startTime: getCurrentTime(),                                                                        // 开始时间
      endTime: getWorkEndTime(startMs),                                                                   // 结束时间（5~15 分钟后）
      startFloor: Math.floor(getRandomInRange(config.startFloor.min, config.startFloor.max)),             // 起始楼层
      numberOfPeople: Math.floor(getRandomInRange(config.numberOfPeople.min, config.numberOfPeople.max)), // 载人数
      remarks: 'Normal operation',                                                                        // 备注
      wind: getRandomInRange(config.wind.min, config.wind.max),                                           // 风速（m/s）
      height: getRandomInRange(config.height.min, config.height.max),                                     // 当前高度（米）
      direction: Math.random() > 0.5 ? 'up' : 'down'                                                      // 运行方向（up/down）
    }
  }
}

// 升降机实时数据：升降机当前运行状态的实时数据
function generateElevatorRealTimeData(config) {
  return {
    code: config.code,
    id: config.id,
    properties: {
      monitorTime: getCurrentTime(),                                                                      // 监测时间
      weightPercent: getRandomInRange(config.weightPercent.min, config.weightPercent.max),                // 载重百分比（%）
      weight: getRandomInRange(config.weight.min, config.weight.max),                                     // 当前重量（吨）
      liftingDirection: '2',                                                                              // 升降方向
      beforeLock: '0',                                                                                    // 前门锁状态
      speed: getRandomInRange(config.speed.min, config.speed.max),                                        // 当前速度（m/s）
      startHeight: getRandomInRange(config.startHeight.min, config.startHeight.max),                      // 起始高度
      endHeight: getRandomInRange(config.endHeight.min, config.endHeight.max),                            // 目标高度
      avgSpeed: getRandomInRange(config.avgSpeed.min, config.avgSpeed.max),                               // 平均速度（m/s）
      xAngle: getRandomInRange(config.xAngle.min, config.xAngle.max),                                     // X轴角度
      afterLock: '0',                                                                                     // 后门锁状态
      yAngle: getRandomInRange(config.yAngle.min, config.yAngle.max),                                     // Y轴角度
      warningState: '0',                                                                                  // 警告状态
      numberOfPeople: Math.floor(getRandomInRange(config.numberOfPeople.min, config.numberOfPeople.max)), // 当前人数
      height: Math.floor(getRandomInRange(config.height.min, config.height.max)),                         // 当前楼层
      wind: getRandomInRange(config.wind.min, config.wind.max)                                            // 风速（m/s）
    }
  }
}

// 高支模监测数据：高支模结构的实时安全监测数据
function generateHighFormworkData(config) {
  return {
    code: config.code,
    id: config.id,
    properties: {
      monitorTime: getCurrentTime(),                                                                      // 监测时间
      tiltStatus: 1,                                                                                      // 倾斜状态（1-正常）
      sensorStatus: 1,                                                                                    // 传感器状态
      batteryStatus: 1,                                                                                   // 电池状态
      xAngle: getRandomInRange(config.xAngle.min, config.xAngle.max),                                     // X轴倾斜角度（度）
      pressure: getRandomInRange(config.pressure.min, config.pressure.max),                               // 压力值（kPa）
      yAngle: getRandomInRange(config.yAngle.min, config.yAngle.max),                                     // Y轴倾斜角度（度）
      batteryLevel: Math.floor(getRandomInRange(config.batteryLevel.min, config.batteryLevel.max)),       // 电池电量（%）
      settlement: getRandomInRange(config.settlement.min, config.settlement.max)                          // 沉降量（mm）
    }
  }
}

// 深基坑监测数据：深基坑工程的综合监测数据
function generateDeepFoundationPitData(config) {
  return {
    code: config.code,
    id: config.id,
    properties: {
      strain: getRandomInRange(config.strain.min, config.strain.max),                                     // 应变值（με）
      monitorTime: getCurrentTime(),                                                                      // 监测时间
      sensorStatus: 1,                                                                                    // 传感器状态
      strainStatus: 1,                                                                                    // 应变状态
      settlementRate: getRandomInRange(config.settlementRate.min, config.settlementRate.max),             // 沉降速率（mm/d）
      earthPressureStatus: 1,                                                                             // 土压力状态
      waterLevelStatus: 1,                                                                                // 水位状态
      settlement: getRandomInRange(config.settlement.min, config.settlement.max),                         // 累计沉降（mm）
      horizontalDisplacement: getRandomInRange(config.horizontalDisplacement.min, config.horizontalDisplacement.max), // 水平位移（mm）
      displacementStatus: 1,                                                                              // 位移状态
      xAngle: getRandomInRange(config.xAngle.min, config.xAngle.max),                                     // X轴角度
      earthPressure: getRandomInRange(config.earthPressure.min, config.earthPressure.max),                // 土压力（kPa）
      yAngle: getRandomInRange(config.yAngle.min, config.yAngle.max),                                     // Y轴角度
      waterLevel: getRandomInRange(config.waterLevel.min, config.waterLevel.max)                          // 水位深度（米）
    }
  }
}

// 环境监测数据：施工现场环境参数的实时监测数据
function generateEnvironmentMonitoringData(config) {
  const windDirs = ['北风', '东北风', '东风', '东南风', '南风', '西南风', '西风', '西北风']
  const windDir = windDirs[Math.floor(Math.random() * windDirs.length)]
  return {
    code: config.code,
    id: config.id,
    properties: {
      monitorTime: getCurrentTime(),                                                                      // 监测时间
      temp: getRandomInRange(config.temp.min, config.temp.max),                                           // 温度（℃）
      sensorStatus: 1,                                                                                    // 传感器状态
      humiStatus: 0,                                                                                      // 湿度状态
      pm10: getRandomInRange(config.pm10.min, config.pm10.max),                                           // PM10浓度（μg/m³）
      windDir: Math.floor(Math.random() * 360),                                                            // 风向角度（度）
      pm25Status: 1,                                                                                      // PM2.5状态
      airPres: getRandomInRange(config.airPres.min, config.airPres.max),                                  // 大气压力（kPa）
      tsp: getRandomInRange(config.tsp.min, config.tsp.max),                                              // 总悬浮颗粒物
      airPresStatus: 1,                                                                                   // 气压状态
      noiseStatus: 1,                                                                                     // 噪声状态
      windDirDesc: windDir,                                                                               // 风向描述
      pm10Status: 1,                                                                                      // PM10状态
      pm25: getRandomInRange(config.pm25.min, config.pm25.max),                                           // PM2.5浓度（μg/m³）
      tempStatus: 2,                                                                                      // 温度状态
      tspStatus: 0,                                                                                       // TSP状态
      windSpeedStatus: 1,                                                                                 // 风速状态
      noise: getRandomInRange(config.noise.min, config.noise.max),                                        // 噪声值（dB）
      humi: getRandomInRange(config.humi.min, config.humi.max),                                           // 湿度（%）
      windSpeed: getRandomInRange(config.windSpeed.min, config.windSpeed.max)                             // 风速（m/s）
    }
  }
}

// 喷淋设备状态数据：喷淋设备的简单状态数据
function generateSprinklerData(config) {
  return {
    code: config.code,
    id: config.id,
    properties: {
      status: Math.floor(getRandomInRange(config.status.min, config.status.max)).toString()               // 设备状态（0-停止，1-运行）
    }
  }
}

export {
  generateTowerCraneWorkData,
  generateTowerCraneRealTimeData,
  generateElevatorWorkData,
  generateElevatorRealTimeData,
  generateHighFormworkData,
  generateDeepFoundationPitData,
  generateEnvironmentMonitoringData,
  generateSprinklerData
}

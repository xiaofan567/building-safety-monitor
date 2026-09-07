<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { io } from 'socket.io-client'

// 设备通信模拟服务端地址（Express + Socket.IO）
const SIM_SERVER_URL = import.meta.env.VITE_SIM_SERVER_URL || 'http://localhost:3000'

// ---------------- 常量 ----------------
const GROUP_NAMES = {
  tower_crane: '塔吊设备',
  elevator: '升降机设备',
  high_formwork: '高支模设备',
  deep_foundation: '深基坑设备',
  environment: '环境监测设备',
  sprinkler: '喷淋设备'
}

// 设备 ID -> 配置键
const CONFIG_KEY_MAP = {
  1: 'towerCraneWork',
  2: 'towerCraneRealTime',
  3: 'elevatorWork',
  4: 'elevatorRealTime',
  5: 'highFormwork',
  6: 'deepFoundation',
  7: 'environment',
  8: 'sprinkler'
}

// 参数显示名称
const PARAM_NAMES = {
  liftingPointHeight: '起吊高度',
  liftingPointHeightUnload: '卸吊高度',
  liftingPointRadius: '起吊半径',
  liftingPointRadiusUnload: '卸吊半径',
  minHeight: '最小高度',
  maxPercent: '最大力矩百分比',
  maxRadius: '最大半径',
  startAngle: '起始角度',
  maxHeight: '最大高度',
  maxWindSpeed: '最大风速',
  endAngle: '结束角度',
  weight: '重量',
  minRadius: '最小半径',
  rotation: '旋转角度',
  moment: '力矩',
  amplitude: '幅度',
  windSpeed: '风速',
  obliquity: '倾斜度',
  height: '高度',
  angleY: 'Y轴角度',
  endFloor: '结束楼层',
  speed: '速度',
  startHeight: '起始高度',
  endHeight: '结束高度',
  angle: '角度',
  startFloor: '起始楼层',
  numberOfPeople: '人数',
  wind: '风力',
  weightPercent: '重量百分比',
  avgSpeed: '平均速度',
  xAngle: 'X轴角度',
  yAngle: 'Y轴角度',
  pressure: '压力',
  batteryLevel: '电池电量',
  settlement: '沉降',
  strain: '应变',
  settlementRate: '沉降率',
  horizontalDisplacement: '水平位移',
  earthPressure: '土压力',
  waterLevel: '水位',
  temp: '温度',
  pm10: 'PM10',
  pm25: 'PM2.5',
  airPres: '气压',
  tsp: '总悬浮颗粒物',
  noise: '噪音',
  humi: '湿度',
  status: '状态',
  monitorTime: '监测时间',
  equipmentId: '设备ID',
  heightAlarm: '高度报警',
  momentAlarm: '力矩报警',
  windSpeedAlarm: '风速报警',
  noAlarm: '无报警状态',
  weightAlarm: '重量报警',
  direction: '运行方向',
  remarks: '备注',
  createTime: '创建时间',
  startTime: '开始时间',
  endTime: '结束时间',
  liftingDirection: '升降方向',
  beforeLock: '前门锁状态',
  afterLock: '后门锁状态',
  warningState: '警告状态',
  tiltStatus: '倾斜状态',
  sensorStatus: '传感器状态',
  batteryStatus: '电池状态',
  strainStatus: '应变状态',
  earthPressureStatus: '土压力状态',
  waterLevelStatus: '水位状态',
  displacementStatus: '位移状态',
  humiStatus: '湿度状态',
  pm25Status: 'PM2.5状态',
  pm10Status: 'PM10状态',
  airPresStatus: '气压状态',
  noiseStatus: '噪声状态',
  tempStatus: '温度状态',
  tspStatus: 'TSP状态',
  windSpeedStatus: '风速状态',
  windDir: '风向角度',
  windDirDesc: '风向描述'
}

// 设备图标（内联 SVG，随运行状态变色）
const DEVICE_ICONS = {
  tower: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><path d="M12 3v13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M5 16h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M12 5l7 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M5 20h14" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><path d="M8 13l-3 3M16 13l3 3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>',
  elevator: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><rect x="7" y="3" width="10" height="18" rx="1.5" stroke="currentColor" stroke-width="1.8"/><path d="M7 9h10M7 15h10" stroke="currentColor" stroke-width="1.2" opacity="0.6"/><path d="M10.5 12.5l1.5-2 1.5 2M10.5 14.5l1.5 2 1.5-2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  formwork: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><path d="M4 8l8-4 8 4v8l-8 4-8-4V8z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M4 8l8 4 8-4M12 12v8" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/></svg>',
  foundation: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><path d="M5 5h14l-2 14H7L5 5z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 8h6M8.5 12h7M8 16h8" stroke="currentColor" stroke-width="1.2" opacity="0.6"/></svg>',
  environment: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><path d="M7 18a4 4 0 010-8 5 5 0 019-2 4.5 4.5 0 013 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/><path d="M12 18v-6M9.5 15.5L12 14l2.5 1.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  sprinkler: '<svg viewBox="0 0 24 24" fill="none" width="34" height="34"><path d="M12 3c3 4 5 6.5 5 9.5a5 5 0 01-10 0C7 9.5 9 7 12 3z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M12 20v1" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>'
}

const ICON_KEY_MAP = { 1: 'tower', 2: 'tower', 3: 'elevator', 4: 'elevator', 5: 'formwork', 6: 'foundation', 7: 'environment', 8: 'sprinkler' }

function getDeviceIcon(deviceId) {
  return DEVICE_ICONS[ICON_KEY_MAP[deviceId]] || DEVICE_ICONS.tower
}

// ---------------- 状态 ----------------
const socket = ref(null)
const socketConnected = ref(false)
const connectionStatus = reactive({})
const deviceStatus = reactive({})
const logs = ref([])
const autoScroll = ref(true)
const logFilter = ref('all')
const deviceConfig = ref({})

const configModalVisible = ref(false)
const currentEditingDevice = ref(null)
const configForm = reactive({ code: '', id: '', params: {} })

const confirmModalVisible = ref(false)
const pendingDisconnectGroup = ref(null)

const logContentRef = ref(null)

// ---------------- 计算属性 ----------------
const groupList = computed(() => Object.keys(GROUP_NAMES))
const deviceIds = computed(() => Object.keys(deviceStatus).map(Number).sort((a, b) => a - b))
const connectedGroupCount = computed(() => groupList.value.filter(g => connectionStatus[g]).length)

const filteredLogs = computed(() => {
  return logs.value.filter(log => {
    if (logFilter.value === 'sent') return log.type === 'sent' || log.type === 'info'
    if (logFilter.value === 'received') return log.type === 'received' || log.type === 'error'
    return true
  })
})

const configModalTitle = computed(() => {
  const id = currentEditingDevice.value
  if (!id) return '设备配置'
  return `${id}. ${deviceStatus[id]?.type || ''} - 数据配置`
})

// ---------------- Socket 通信 ----------------
function replaceObject(target, source) {
  Object.keys(target).forEach(k => delete target[k])
  Object.assign(target, source)
}

function setupSocket() {
  socket.value = io(SIM_SERVER_URL)

  socket.value.on('connect', () => {
    socketConnected.value = true
    addLogEntry('info', 'Socket 服务连接成功')
  })

  socket.value.on('disconnect', () => {
    socketConnected.value = false
    addLogEntry('warning', 'Socket 服务连接已断开')
  })

  // 初始化消息：连接状态 + 设备状态
  socket.value.on('initial_status', (data) => {
    replaceObject(connectionStatus, data.connectionStatus || {})
    replaceObject(deviceStatus, data.deviceStatus || {})
    addLogEntry('info', '已获取设备初始化状态')
  })

  // 连接状态更新
  socket.value.on('connection_status', (data) => {
    connectionStatus[data.group] = data.connected
  })

  // 设备运行状态更新
  socket.value.on('device_status_changed', (data) => {
    if (deviceStatus[data.deviceId]) {
      deviceStatus[data.deviceId].running = data.running
    }
  })

  // 数据发送日志
  socket.value.on('data_sent', (data) => {
    addLogEntry('sent', `设备 ${data.deviceId} 发送数据: ${JSON.stringify(data.data)}`, data.timestamp)
  })

  // 服务器响应
  socket.value.on('server_response', (data) => {
    addLogEntry('received', `服务器响应 (${GROUP_NAMES[data.group] || data.group}): ${data.message}`)
  })

  // 错误处理
  socket.value.on('error', (data) => {
    addLogEntry('error', data.message || '服务端发生错误')
  })
}

// ---------------- 设备控制 ----------------
function toggleDevice(deviceId) {
  if (!socket.value?.connected) {
    ElMessage.warning('Socket 服务未连接')
    return
  }
  socket.value.emit('toggle_device', { deviceId })
  addLogEntry('info', `操作设备 ${deviceId}`)
}

function showDisconnectConfirm(group) {
  pendingDisconnectGroup.value = group
  confirmModalVisible.value = true
}

function confirmDisconnect() {
  if (pendingDisconnectGroup.value) {
    const group = pendingDisconnectGroup.value
    socket.value?.emit('disconnect_group', { group })
    addLogEntry('info', `断开连接: ${GROUP_NAMES[group]}`)
  }
  confirmModalVisible.value = false
  pendingDisconnectGroup.value = null
}

// ---------------- 配置编辑 ----------------
function showConfigModal(deviceId) {
  const configKey = CONFIG_KEY_MAP[deviceId]
  const config = deviceConfig.value[configKey]
  if (!config) {
    ElMessage.warning('该设备暂无配置选项')
    return
  }
  currentEditingDevice.value = deviceId
  configForm.code = config.code ?? ''
  configForm.id = config.id ?? ''
  configForm.params = {}
  Object.entries(config).forEach(([key, val]) => {
    if (key !== 'code' && key !== 'id' && val && typeof val === 'object' && val.min !== undefined) {
      configForm.params[key] = { min: val.min, max: val.max }
    }
  })
  configModalVisible.value = true
}

function saveDeviceConfig() {
  const deviceId = currentEditingDevice.value
  if (!deviceId) return
  const configKey = CONFIG_KEY_MAP[deviceId]
  const config = { ...deviceConfig.value[configKey] }

  let hasChanges = false
  if (configForm.code !== config.code) {
    config.code = configForm.code
    hasChanges = true
  }
  const newId = isNaN(configForm.id) || configForm.id === '' ? configForm.id : Number(configForm.id)
  if (newId !== config.id) {
    config.id = newId
    hasChanges = true
  }
  Object.keys(configForm.params).forEach(key => {
    const { min, max } = configForm.params[key]
    if (config[key] && (min !== config[key].min || max !== config[key].max)) {
      config[key] = { min, max }
      hasChanges = true
    }
  })

  if (!hasChanges) {
    configModalVisible.value = false
    return
  }

  deviceConfig.value[configKey] = config

  fetch(`${SIM_SERVER_URL}/api/config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(deviceConfig.value)
  })
    .then(res => res.json())
    .then(result => {
      if (result.success) {
        addLogEntry('success', `设备 ${deviceId} 配置已保存`)
        configModalVisible.value = false
      } else {
        addLogEntry('error', '保存配置失败')
      }
    })
    .catch(() => {
      addLogEntry('error', '保存配置时发生错误')
    })
}

// ---------------- 日志 ----------------
function addLogEntry(type, message, timestamp = new Date().toISOString()) {
  logs.value.push({ type, message, timestamp })
}

function clearLogs() {
  logs.value = []
}

function toggleAutoScroll() {
  autoScroll.value = !autoScroll.value
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString('zh-CN', { hour12: false })
}

// 自动滚动到底部
watch(filteredLogs, () => {
  if (!autoScroll.value) return
  nextTick(() => {
    if (logContentRef.value) {
      logContentRef.value.scrollTop = logContentRef.value.scrollHeight
    }
  })
})

// ---------------- 工具 ----------------
function getDeviceStatusText(deviceId, running) {
  if (deviceId === 2 || deviceId === 4) {
    return running ? '运行中，点击停止' : '已停止，点击启动'
  } else if (deviceId === 8) {
    return running ? '已连接，点击断开' : '未连接，点击连接'
  } else {
    return running ? '数据已发送' : '点击发送数据'
  }
}

function getParameterDisplayName(key) {
  return PARAM_NAMES[key] || key
}

async function loadDeviceConfig() {
  try {
    const res = await fetch(`${SIM_SERVER_URL}/api/config`)
    deviceConfig.value = await res.json()
  } catch (e) {
    console.error('加载设备配置失败:', e)
    deviceConfig.value = {}
  }
}

// ---------------- 生命周期 ----------------
onMounted(async () => {
  await loadDeviceConfig()
  setupSocket()
})

onUnmounted(() => {
  socket.value?.disconnect()
})
</script>

<template>
  <div class="simulation-page">
    <!-- 顶部标题 -->
    <div class="page-header">
      <h2>设备通信模拟控制</h2>
      <div class="ws-status">
        <span class="ws-dot" :class="{ connected: socketConnected }"></span>
        <span>{{ socketConnected ? 'Socket 已连接' : 'Socket 未连接' }}</span>
      </div>
    </div>

    <div class="sim-main">
      <!-- 左侧：连接状态 -->
      <section class="panel status-panel">
        <div class="panel-header">
          <span class="panel-title">连接状态</span>
          <span class="panel-count">{{ connectedGroupCount }} / {{ groupList.length }} 已连接</span>
        </div>
        <div class="status-list">
          <div
            v-for="group in groupList"
            :key="group"
            class="status-item"
            :class="connectionStatus[group] ? 'connected' : 'disconnected'"
          >
            <div class="status-item-info">
              <span class="status-name">{{ GROUP_NAMES[group] }}</span>
              <span class="status-badge" :class="connectionStatus[group] ? 'on' : 'off'">
                {{ connectionStatus[group] ? '已连接' : '未连接' }}
              </span>
            </div>
            <button
              v-if="connectionStatus[group]"
              class="disconnect-btn"
              @click="showDisconnectConfirm(group)"
            >
              断开连接
            </button>
          </div>
          <div v-if="groupList.length === 0" class="panel-empty">等待服务端初始化数据...</div>
        </div>
      </section>

      <!-- 右侧：设备控制 -->
      <section class="panel control-panel">
        <div class="panel-header">
          <span class="panel-title">设备控制</span>
          <span class="panel-count">{{ deviceIds.length }} 台设备</span>
        </div>
        <div class="device-grid">
          <div v-for="id in deviceIds" :key="id" class="device-item">
            <button
              class="device-button"
              :class="deviceStatus[id]?.running ? 'running' : 'stopped'"
              @click="toggleDevice(id)"
            >
              <span class="device-icon" v-html="getDeviceIcon(id)"></span>
              <span class="device-button-inner">
                <span class="device-name">{{ id }}. {{ deviceStatus[id]?.type }}</span>
                <span class="device-status-text">{{ getDeviceStatusText(id, deviceStatus[id]?.running) }}</span>
              </span>
            </button>
            <button class="edit-btn" @click="showConfigModal(id)">编辑</button>
          </div>
          <div v-if="deviceIds.length === 0" class="panel-empty">等待服务端初始化数据...</div>
        </div>
      </section>
    </div>

    <!-- 底部：通信日志 -->
    <section class="panel log-panel">
      <div class="panel-header">
        <span class="panel-title">通信日志</span>
        <div class="log-controls">
          <el-select v-model="logFilter" size="small" class="filter-select">
            <el-option label="全部" value="all" />
            <el-option label="仅发送" value="sent" />
            <el-option label="仅接收" value="received" />
          </el-select>
          <button class="log-btn" @click="clearLogs">清空日志</button>
          <button class="log-btn" @click="toggleAutoScroll">自动滚动: {{ autoScroll ? '开启' : '关闭' }}</button>
        </div>
      </div>
      <div class="log-content" ref="logContentRef">
        <div v-for="(log, i) in filteredLogs" :key="i" class="log-entry" :class="log.type">
          <span class="log-time">[{{ formatTime(log.timestamp) }}]</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
        <div v-if="filteredLogs.length === 0" class="log-empty">暂无日志</div>
      </div>
    </section>

    <!-- 配置编辑弹窗 -->
    <el-dialog
      v-model="configModalVisible"
      :title="configModalTitle"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form label-width="120px">
        <div class="config-section-title">基本信息</div>
        <el-form-item label="设备编码 (code)">
          <el-input v-model="configForm.code" placeholder="设备编码" />
        </el-form-item>
        <el-form-item label="设备ID (id)">
          <el-input v-model="configForm.id" placeholder="设备ID" />
        </el-form-item>
        <template v-if="Object.keys(configForm.params).length > 0">
          <div class="config-section-title">参数范围配置</div>
          <el-form-item
            v-for="(param, key) in configForm.params"
            :key="key"
            :label="getParameterDisplayName(key)"
          >
            <div class="range-inputs">
              <el-input-number v-model="configForm.params[key].min" :controls="false" size="small" class="range-input" />
              <span class="range-sep">~</span>
              <el-input-number v-model="configForm.params[key].max" :controls="false" size="small" class="range-input" />
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="configModalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDeviceConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 断开连接确认弹窗 -->
    <el-dialog v-model="confirmModalVisible" title="确认断开连接" width="420px">
      <p class="confirm-text">
        确定要断开
        <span class="highlight">{{ pendingDisconnectGroup ? GROUP_NAMES[pendingDisconnectGroup] : '' }}</span>
        的连接吗？
      </p>
      <template #footer>
        <el-button @click="confirmModalVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDisconnect">确定断开</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.simulation-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e8edf2;
  letter-spacing: 1px;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8899aa;
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5a6a7a;
  transition: all 0.3s;
}

.ws-dot.connected {
  background: #0d9e6c;
  box-shadow: 0 0 6px rgba(13, 158, 108, 0.8);
}

.sim-main {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.panel {
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.status-panel {
  width: 320px;
  flex-shrink: 0;
}

.control-panel {
  flex: 1;
}

.log-panel {
  margin-top: 16px;
  height: 240px;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(26, 50, 72, 0.5);
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #e8edf2;
  position: relative;
  padding-left: 14px;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: linear-gradient(180deg, #1a73e8, #0d9e6c);
  border-radius: 2px;
}

.panel-count {
  font-size: 11px;
  color: #5a6a7a;
}

.panel-empty {
  color: #3a5068;
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
}

/* ---------- 连接状态 ---------- */
.status-list {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-list::-webkit-scrollbar,
.device-grid::-webkit-scrollbar,
.log-content::-webkit-scrollbar {
  width: 3px;
}

.status-list::-webkit-scrollbar-thumb,
.device-grid::-webkit-scrollbar-thumb,
.log-content::-webkit-scrollbar-thumb {
  background: rgba(26, 115, 232, 0.15);
  border-radius: 2px;
}

.status-item {
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid;
  transition: all 0.2s;
}

.status-item.connected {
  background: rgba(13, 158, 108, 0.08);
  border-color: rgba(13, 158, 108, 0.45);
}

.status-item.disconnected {
  background: #0a1625;
  border-color: rgba(26, 50, 72, 0.5);
}

.status-item-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.status-name {
  font-size: 13px;
  color: #bcc8d4;
  font-weight: 500;
  white-space: nowrap;
}

.status-item.connected .status-name {
  color: #e8edf2;
}

.status-badge {
  align-self: flex-start;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
}

.status-badge.on {
  background: rgba(13, 158, 108, 0.15);
  color: #4ade80;
}

.status-badge.off {
  background: rgba(26, 50, 72, 0.4);
  color: #5a6a7a;
}

.disconnect-btn {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 11px;
  border: 1px solid rgba(229, 69, 69, 0.4);
  background: transparent;
  color: #ff6b6b;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s;
}

.disconnect-btn:hover {
  background: rgba(229, 69, 69, 0.12);
}

/* ---------- 设备控制 ---------- */
.device-grid {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  align-content: start;
}

.device-item {
  display: flex;
  gap: 10px;
  align-items: stretch;
}

.device-button {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: all 0.25s;
  min-width: 0;
}

.device-button.running {
  background: linear-gradient(135deg, rgba(13, 158, 108, 0.16), rgba(13, 158, 108, 0.05));
  border-color: rgba(13, 158, 108, 0.45);
  color: #4ade80;
}

.device-button.stopped {
  background: #0a1625;
  border-color: rgba(26, 50, 72, 0.5);
  color: #8899aa;
}

.device-button.stopped:hover {
  border-color: rgba(26, 115, 232, 0.35);
  color: #bcc8d4;
}

.device-icon {
  display: flex;
  flex-shrink: 0;
}

.device-button-inner {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.device-name {
  font-size: 13px;
  font-weight: 600;
  color: #e8edf2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-button.stopped .device-name {
  color: #bcc8d4;
}

.device-status-text {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.85;
}

.edit-btn {
  width: 52px;
  border: 1px solid rgba(26, 115, 232, 0.3);
  border-radius: 8px;
  background: rgba(26, 115, 232, 0.12);
  color: #5a8fd4;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.edit-btn:hover {
  background: rgba(26, 115, 232, 0.22);
  color: #1a73e8;
}

/* ---------- 通信日志 ---------- */
.log-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-select {
  width: 110px;
}

.log-btn {
  padding: 5px 12px;
  font-size: 12px;
  background: #0a1625;
  border: 1px solid rgba(26, 50, 72, 0.5);
  color: #8899aa;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s;
}

.log-btn:hover {
  color: #bcc8d4;
  border-color: rgba(26, 115, 232, 0.3);
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: #0a1625;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.8;
}

.log-entry {
  display: flex;
  gap: 10px;
  word-break: break-all;
  padding: 2px 0;
}

.log-time {
  color: #3a5068;
  flex-shrink: 0;
}

.log-message {
  color: #bcc8d4;
}

.log-entry.sent .log-message {
  color: #68d391;
}

.log-entry.received .log-message {
  color: #90cdf4;
}

.log-entry.success .log-message {
  color: #4ade80;
}

.log-entry.error .log-message {
  color: #fc8181;
}

.log-entry.warning .log-message {
  color: #f6e05e;
}

.log-empty {
  color: #3a5068;
  text-align: center;
  padding: 30px 0;
}

/* ---------- 弹窗内部 ---------- */
.config-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a73e8;
  margin: 6px 0 14px;
  padding-left: 10px;
  border-left: 3px solid #1a73e8;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-input {
  width: 200px;
}

.range-sep {
  color: #5a6a7a;
}

.confirm-text {
  color: #bcc8d4;
  font-size: 14px;
  line-height: 1.8;
}

.confirm-text .highlight {
  color: #ff6b6b;
  font-weight: 600;
}

/* ---------- Element Plus 暗色适配 ---------- */
:deep(.el-dialog) {
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.6);
  border-radius: 10px;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(26, 50, 72, 0.5);
  margin-right: 0;
  padding: 16px 20px;
}

:deep(.el-dialog__title) {
  color: #e8edf2;
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px;
  color: #bcc8d4;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid rgba(26, 50, 72, 0.5);
  padding: 12px 20px;
}

:deep(.el-form-item__label) {
  color: #8899aa;
  font-size: 13px;
}

:deep(.el-input__wrapper),
:deep(.el-input-number) {
  background: #0a1625;
  border-color: rgba(26, 50, 72, 0.4);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input-number:hover) {
  border-color: rgba(26, 115, 232, 0.3);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px rgba(26, 115, 232, 0.2);
}

:deep(.el-input__inner) {
  color: #bcc8d4;
}

:deep(.el-select__wrapper) {
  background: #0a1625;
  box-shadow: 0 0 0 1px rgba(26, 50, 72, 0.4) inset;
}

:deep(.el-select__placeholder),
:deep(.el-select__selected-item) {
  color: #bcc8d4;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #1a73e8;
  --el-button-border-color: #1a73e8;
}

:deep(.el-button--primary:hover) {
  --el-button-bg-color: #1557b0;
  --el-button-border-color: #1557b0;
}

:deep(.el-button--danger) {
  --el-button-bg-color: #e54545;
  --el-button-border-color: #e54545;
}

:deep(.el-button--danger:hover) {
  --el-button-bg-color: #c93030;
  --el-button-border-color: #c93030;
}

:deep(.el-button) {
  --el-button-bg-color: #0a1625;
  --el-button-border-color: rgba(26, 50, 72, 0.4);
  --el-button-text-color: #8899aa;
}

:deep(.el-button:hover) {
  --el-button-border-color: rgba(26, 115, 232, 0.3);
  --el-button-text-color: #bcc8d4;
}

/* 下拉选项弹层（teleport 到 body，需全局样式） */
:global(.el-select-dropdown) {
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.6);
}

:global(.el-select-dropdown__item) {
  color: #bcc8d4;
}

:global(.el-select-dropdown__item.is-hovering) {
  background: rgba(26, 115, 232, 0.1);
}

:global(.el-select-dropdown__item.is-selected) {
  color: #1a73e8;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1200px) {
  .status-panel {
    width: 280px;
  }
}

@media (max-width: 900px) {
  .sim-main {
    flex-direction: column;
  }

  .status-panel {
    width: 100%;
    flex: 1;
  }

  .device-grid {
    grid-template-columns: 1fr;
  }
}
</style>

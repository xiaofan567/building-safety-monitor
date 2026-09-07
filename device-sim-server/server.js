/**
 * 设备通信模拟服务端
 *
 * 基于 Express + Socket.IO 实现：
 *  - 模拟 6 个设备组（塔吊/升降机/高支模/深基坑/环境/喷淋）的 TCP 连接状态
 *  - 模拟 8 台设备的单次/周期数据发送，数据范围取自设备配置
 *  - 提供 GET/POST /api/config 接口，将设备配置以 JSON 文件形式持久化
 *
 * 事件约定（与前端页面 DeviceSimulation.vue 对应）：
 *  服务端 -> 客户端：initial_status / connection_status / device_status_changed / data_sent / server_response / error
 *  客户端 -> 服务端：toggle_device { deviceId } / disconnect_group { group }
 */
import express from 'express'
import http from 'http'
import cors from 'cors'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { Server } from 'socket.io'
import * as dataGenerators from './data-generators.js'
import TcpDeviceClient from './tcp-client.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PORT = process.env.PORT || 3000
const CONFIG_PATH = path.join(__dirname, 'config', 'device-config.json')

// ---------------- 设备定义 ----------------
const GROUP_NAMES = {
  tower_crane: '塔吊设备',
  elevator: '升降机设备',
  high_formwork: '高支模设备',
  deep_foundation: '深基坑设备',
  environment: '环境监测设备',
  sprinkler: '喷淋设备'
}

// mode: once=单次发送，periodic=周期发送
const DEVICES = {
  1: { type: '塔机作业（单次发送）', group: 'tower_crane', configKey: 'towerCraneWork', mode: 'once' },
  2: { type: '塔机实时数据（周期发送）', group: 'tower_crane', configKey: 'towerCraneRealTime', mode: 'periodic', interval: 3000 },
  3: { type: '升降机作业（单次发送）', group: 'elevator', configKey: 'elevatorWork', mode: 'once' },
  4: { type: '升降机实时数据（周期发送）', group: 'elevator', configKey: 'elevatorRealTime', mode: 'periodic', interval: 3000 },
  5: { type: '高支模（单次发送）', group: 'high_formwork', configKey: 'highFormwork', mode: 'once' },
  6: { type: '深基坑（单次发送）', group: 'deep_foundation', configKey: 'deepFoundation', mode: 'once' },
  7: { type: '环境监测', group: 'environment', configKey: 'environment', mode: 'periodic', interval: 5000 },
  8: { type: '喷淋设备', group: 'sprinkler', configKey: 'sprinkler', mode: 'periodic', interval: 4000 }
}

// 设备 ID -> 数据生成器（与 data-generators.js 对应）
const GENERATORS = {
  1: dataGenerators.generateTowerCraneWorkData,
  2: dataGenerators.generateTowerCraneRealTimeData,
  3: dataGenerators.generateElevatorWorkData,
  4: dataGenerators.generateElevatorRealTimeData,
  5: dataGenerators.generateHighFormworkData,
  6: dataGenerators.generateDeepFoundationPitData,
  7: dataGenerators.generateEnvironmentMonitoringData,
  8: dataGenerators.generateSprinklerData
}

// 根据设备 ID 生成模拟数据（返回 { code, id, properties }）
function generateData(deviceId) {
  const dev = DEVICES[deviceId]
  const config = deviceConfig[dev?.configKey] || {}
  const generator = GENERATORS[deviceId]
  return generator ? generator(config) : { code: '', id: '', properties: {} }
}

// ---------------- 运行状态 ----------------
const connectionStatus = {}
for (const group of Object.keys(GROUP_NAMES)) {
  connectionStatus[group] = false
}

const deviceStatus = {}
for (const [id, dev] of Object.entries(DEVICES)) {
  deviceStatus[id] = { type: dev.type, group: dev.group, running: false, timer: null }
}

// ---------------- 配置加载/保存 ----------------
let deviceConfig = {}

function loadConfig() {
  try {
    const raw = fs.readFileSync(CONFIG_PATH, 'utf-8')
    deviceConfig = JSON.parse(raw)
    console.log('[config] 已加载配置文件:', CONFIG_PATH)
  } catch (e) {
    console.error('[config] 加载配置失败，使用空配置:', e.message)
    deviceConfig = {}
  }
}

function saveConfig() {
  try {
    fs.mkdirSync(path.dirname(CONFIG_PATH), { recursive: true })
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(deviceConfig, null, 2), 'utf-8')
    return true
  } catch (e) {
    console.error('[config] 保存配置失败:', e.message)
    return false
  }
}

// ---------------- 事件推送 ----------------
let io = null

// 每个设备组对应一个真实 TCP 客户端（向后端 Netty 服务上报数据）
const tcpClients = {}
for (const group of Object.keys(GROUP_NAMES)) {
  tcpClients[group] = new TcpDeviceClient(GROUP_NAMES[group])
}

function broadcastStatus(group, connected) {
  io.emit('connection_status', { group, connected })
  io.emit('server_response', { group, message: connected ? 'TCP 连接已建立' : 'TCP 连接已断开' })
  // 同步真实 TCP 连接：设备组连接建立时连接后端 Netty，断开时关闭
  if (connected) {
    tcpClients[group].connect()
  } else {
    tcpClients[group].close()
  }
}

function sendDeviceData(deviceId) {
  const dev = DEVICES[deviceId]
  const data = generateData(deviceId)
  io.emit('data_sent', { deviceId, data, timestamp: new Date().toISOString() })
  io.emit('server_response', { group: dev.group, message: `已收到${dev.type}数据并转发` })
  // 通过该设备组的真实 TCP 连接向后端 Netty 服务上报数据
  tcpClients[dev.group].send(data)
}

function stopDevice(deviceId) {
  const st = deviceStatus[deviceId]
  if (!st || !st.running) return
  st.running = false
  if (st.timer) {
    clearInterval(st.timer)
    st.timer = null
  }
  io.emit('device_status_changed', { deviceId, running: false })
}

function startDevice(deviceId) {
  const dev = DEVICES[deviceId]
  const st = deviceStatus[deviceId]
  if (!dev || st.running) return
  st.running = true
  // 设备启动后，其所在设备组 TCP 连接视为已建立
  if (!connectionStatus[dev.group]) {
    connectionStatus[dev.group] = true
    broadcastStatus(dev.group, true)
  }
  sendDeviceData(deviceId)
  if (dev.mode === 'periodic') {
    st.timer = setInterval(() => sendDeviceData(deviceId), dev.interval)
  }
  io.emit('device_status_changed', { deviceId, running: true })
}

function toggleDevice(deviceId) {
  const st = deviceStatus[deviceId]
  if (!st) return
  if (st.running) {
    stopDevice(deviceId)
  } else {
    startDevice(deviceId)
  }
}

function disconnectGroup(group) {
  if (!GROUP_NAMES[group]) return
  connectionStatus[group] = false
  // 断开连接时，停止该组所有运行中的设备
  Object.keys(DEVICES).forEach((id) => {
    if (DEVICES[id].group === group) {
      stopDevice(Number(id))
    }
  })
  broadcastStatus(group, false)
}

// ---------------- HTTP 接口 ----------------
const app = express()
app.use(cors())
app.use(express.json())

// 获取设备整体配置
app.get('/api/config', (req, res) => {
  res.json(deviceConfig)
})

// 保存设备整体配置（以文件形式落盘）
app.post('/api/config', (req, res) => {
  deviceConfig = req.body || {}
  const ok = saveConfig()
  res.json({ success: ok, message: ok ? '配置已保存' : '配置保存失败' })
})

// 健康检查（便于调试）
app.get('/api/status', (req, res) => {
  const publicDeviceStatus = {}
  Object.entries(deviceStatus).forEach(([id, st]) => {
    publicDeviceStatus[id] = { type: st.type, group: st.group, running: st.running }
  })
  res.json({ ok: true, connectionStatus, deviceStatus: publicDeviceStatus })
})

// 数据生成接口：根据设备 ID 返回模拟数据（1~8）
app.get('/api/data/:deviceId', (req, res) => {
  const deviceId = parseInt(req.params.deviceId, 10)
  if (!DEVICES[deviceId]) {
    return res.status(404).json({ error: '未知设备' })
  }
  res.json(generateData(deviceId))
})

// ---------------- Socket.IO ----------------
const server = http.createServer(app)
io = new Server(server, {
  cors: { origin: '*' }
})

io.on('connection', (socket) => {
  console.log('[socket] 客户端已连接:', socket.id)

  // 发送初始化状态（连接状态 + 设备状态）
  const publicDeviceStatus = {}
  Object.entries(deviceStatus).forEach(([id, st]) => {
    publicDeviceStatus[id] = { type: st.type, group: st.group, running: st.running }
  })
  socket.emit('initial_status', { connectionStatus, deviceStatus: publicDeviceStatus })

  // 设备控制：启动/停止数据发送
  socket.on('toggle_device', ({ deviceId }) => {
    const id = Number(deviceId)
    console.log('[socket] 收到设备控制指令: deviceId =', id)
    toggleDevice(id)
  })

  // 断开设备组 TCP 连接
  socket.on('disconnect_group', ({ group }) => {
    console.log('[socket] 收到断开连接指令: group =', group)
    disconnectGroup(group)
  })

  socket.on('disconnect', () => {
    console.log('[socket] 客户端已断开:', socket.id)
  })
})

loadConfig()

server.listen(PORT, () => {
  console.log(`设备通信模拟服务端已启动: http://localhost:${PORT}`)
  console.log(`已注册设备组: ${Object.keys(GROUP_NAMES).join(', ')}`)
})

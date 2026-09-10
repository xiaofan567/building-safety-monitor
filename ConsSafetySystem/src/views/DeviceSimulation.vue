<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { io } from 'socket.io-client'
// 引入 three.js 及其插件（均为全局脚本，顺序执行后挂载到 window.THREE）
import '../js/three.min.js'
import '../js/OrbitControls.js'
import '../js/GLTFLoader.js'

const THREE = window.THREE

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

// ---------------- 3D 状态（塔吊模型） ----------------
const threeContainer = ref(null)
const isTower = computed(() => currentEditingDevice.value === 1 || currentEditingDevice.value === 2)
// 3D 交互模式：false=旋转视角，true=拖动模型
const dragMode = ref(false)

function setDragMode(v) {
  dragMode.value = v
  if (controls) {
    controls.enableRotate = !v
    controls.domElement.style.cursor = v ? 'grab' : 'default'
  }
}

// 角度转弧度
function degreesToRadians(degrees) {
  return degrees * (Math.PI / 180)
}

// 前臂小车映射函数：0~50 米 -> -3400~3400
function mapTrolleyPosition(value) {
  const clampedValue = Math.max(0, Math.min(50, value))
  return (clampedValue - 0) * (3400 - (-3400)) / (50 - 0) + (-3400)
}

// 吊钩映射函数：0~60 米 -> -4600~1600
function mapHookHeight(value) {
  const clampedValue = Math.max(0, Math.min(60, value))
  return (clampedValue - 0) * (1600 - (-4600)) / (60 - 0) + (-4600)
}

// 递归查找指定名称的部件
function findNestedPart(node, targetName, callback) {
  if (node.name === targetName) {
    callback(node)
    return true
  }
  for (let child of node.children) {
    if (findNestedPart(child, targetName, callback)) return true
  }
  return false
}

// three 场景对象
let scene, camera, renderer, controls
let plane002 = null // 塔吊主部件（可旋转）
let trolley = null // 前臂小车
let hook = null // 吊钩
let rope = null // 吊线
let modelLoaded = false
const rotationCenter = new THREE.Vector3(0, -14, 14)

let showTC = false // 是否播放动画
let animationFrame = 601 // 当前动画帧（601 表示未播放）
let animId = null
let modelDragged = false // 是否被用户手动拖动过

// 塔吊参数平均值
let liftingPointHeightAvg = 0
let liftingPointHeightUnloadAvg = 0
let liftingPointRadiusAvg = 0
let liftingPointRadiusUnloadAvg = 0
let startAngleAvg = 0
let endAngleAvg = 0

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

// ---------------- 3D 模型初始化与动画 ----------------
function init3D() {
  modelDragged = false // 每次重建场景都从默认姿态开始
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xFAF6EE)
  camera = new THREE.PerspectiveCamera(75, 500 / 480, 0.1, 2000)
  camera.position.set(50, 30, 50)
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(500, 480)
  // 控制器，启用阻尼效果
  controls = new THREE.OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  // 启用"左键拖动模型"：空处拖动旋转视角，抓握模型可平移到拖动位置
  enableModelDrag()
  // 光源，双光源避免暗面
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.9)
  directionalLight.position.set(20, 30, 25)
  scene.add(directionalLight)
  // 加载塔吊 glb 模型
  const loader = new THREE.GLTFLoader()
  loader.load(
    '/tdcfv2.glb',
    function (gltf) {
      const model = gltf.scene
      scene.add(model)
      console.log('模型加载成功')
      // 查找主部件
      findNestedPart(model, 'Plane002', function (part) {
        plane002 = part
        console.log('找到 Plane002 主部件')
        part.traverse(child => {
          switch (child.name) {
            case 'Plane002_Material_#44_0001':
              trolley = child
              break
            case 'Plane002_Material_#44_0002':
              hook = child
              break
            case 'Plane002_Material_#44_0003':
              rope = child
              break
          }
        })
        if (!trolley) console.warn('未找到小车组件(Plane002_Material_#44_0001)')
        if (!hook) console.warn('未找到吊钩组件(Plane002_Material_#44_0002)')
        if (!rope) console.warn('未找到吊线组件(Plane002_Material_#44_0003)')
        modelLoaded = true
      })
    },
    function (xhr) {
      console.log((xhr.loaded / xhr.total * 100) + '% 已加载')
    },
    function (error) {
      console.error('加载出错:', error)
    }
  )
}

// 拖拽交互：由"旋转/拖动"模式按钮切换。
// 旋转模式 → OrbitControls 左键旋转视角；拖动模式 → 左键在地面平面平移塔吊
function enableModelDrag() {
  const dom = renderer.domElement
  const raycaster = new THREE.Raycaster()
  const pointer = new THREE.Vector2()
  // 地面平面（y=0），用于把模型约束在"地面"上水平拖动
  const groundPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0)
  const groundPoint = new THREE.Vector3()
  const dragOffset = new THREE.Vector3()
  let dragging = false

  function setPointer(e) {
    const rect = dom.getBoundingClientRect()
    pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
  }

  dom.addEventListener('pointerdown', (e) => {
    if (!plane002 || !dragMode.value || e.button !== 0) return
    e.preventDefault()
    setPointer(e)
    raycaster.setFromCamera(pointer, camera)
    if (!raycaster.ray.intersectPlane(groundPlane, groundPoint)) return
    dragging = true
    modelDragged = true
    dom.style.cursor = 'grabbing'
    dragOffset.subVectors(plane002.position, groundPoint)
  })

  dom.addEventListener('pointermove', (e) => {
    if (!dragging) return
    e.preventDefault()
    setPointer(e)
    raycaster.setFromCamera(pointer, camera)
    if (raycaster.ray.intersectPlane(groundPlane, groundPoint)) {
      plane002.position.set(groundPoint.x + dragOffset.x, plane002.position.y, groundPoint.z + dragOffset.z)
    }
  })

  const onUp = () => {
    if (dragging) {
      dragging = false
      dom.style.cursor = dragMode.value ? 'grab' : 'default'
    }
  }
  dom.addEventListener('pointerup', onUp)
  dom.addEventListener('pointerleave', onUp)
}

// 更新塔吊旋转角度
function updateRotationAngle(targetRadians) {
  if (!plane002) return
  // 若用户拖动过模型，则以拖放后的模型中心为旋转轴，避免被拉回原位
  const pivot = modelDragged ? plane002.position : rotationCenter
  const delta = targetRadians - plane002.rotation.y
  const offset = new THREE.Vector3().subVectors(plane002.position, pivot)
  offset.applyAxisAngle(new THREE.Vector3(0, 1, 0), delta)
  plane002.rotation.y = targetRadians
  plane002.position.copy(pivot).add(offset)
}

// 更新小车位置和吊钩
function updateTrolleyPosition(trolleyPosition, hookHeight) {
  console.log('更新小车位置:', trolleyPosition, hookHeight)
  if (!trolley) return
  trolley.position.set(trolleyPosition, 0, 0)
  updateHookAndRope(hookHeight)
}

// 更新吊钩和模型自带吊线
function updateHookAndRope(hookHeight) {
  if (!hook || !trolley || !rope) return
  hook.position.x = trolley.position.x
  hook.position.y = hookHeight + 100 // +100 防止穿模
  const box = new THREE.Box3().setFromObject(rope)
  const originalHeight = box.max.y - box.min.y // 初始高度
  rope.scale.y = 1 - hookHeight / 1600
  rope.position.y = originalHeight * (1 - rope.scale.y) / 2
  rope.position.x = trolley.position.x
}

// 动画循环
function animate() {
  // 只有在播放状态才继续调度下一帧
  if (showTC) {
    animId = requestAnimationFrame(animate)
  }
  // 600 帧为结束帧
  if (animationFrame <= 600) {
    const targetAngle = startAngleAvg + animationFrame * ((endAngleAvg - startAngleAvg) / 600)
    const targetPointRadius = liftingPointRadiusAvg + animationFrame * ((liftingPointRadiusUnloadAvg - liftingPointRadiusAvg) / 600)
    const targetPointHeight = liftingPointHeightAvg + animationFrame * ((liftingPointHeightUnloadAvg - liftingPointHeightAvg) / 600)
    updateRotationAngle(degreesToRadians(targetAngle))
    updateTrolleyPosition(mapTrolleyPosition(targetPointRadius), mapHookHeight(targetPointHeight))
    animationFrame++
  }
  controls.update()
  renderer.render(scene, camera)
}

// 从表单参数计算平均值并同步到动画变量
function syncFromParams() {
  const mid = (param) => (param && !isNaN(param.min) && !isNaN(param.max)) ? (param.min + param.max) / 2 : null
  const params = configForm.params
  if (currentEditingDevice.value === 1) {
    for (const [key, target] of [
      ['liftingPointHeight', () => { liftingPointHeightAvg = mid(params.liftingPointHeight) ?? liftingPointHeightAvg }],
      ['liftingPointHeightUnload', () => { liftingPointHeightUnloadAvg = mid(params.liftingPointHeightUnload) ?? liftingPointHeightUnloadAvg }],
      ['liftingPointRadius', () => { liftingPointRadiusAvg = mid(params.liftingPointRadius) ?? liftingPointRadiusAvg }],
      ['liftingPointRadiusUnload', () => { liftingPointRadiusUnloadAvg = mid(params.liftingPointRadiusUnload) ?? liftingPointRadiusUnloadAvg }],
      ['startAngle', () => { startAngleAvg = mid(params.startAngle) ?? startAngleAvg }],
      ['endAngle', () => { endAngleAvg = mid(params.endAngle) ?? endAngleAvg }]
    ]) { if (params[key]) target() }
  } else if (currentEditingDevice.value === 2) {
    if (params.rotation) endAngleAvg = mid(params.rotation) ?? endAngleAvg
    if (params.height) liftingPointHeightUnloadAvg = mid(params.height) ?? liftingPointHeightUnloadAvg
    if (params.amplitude) liftingPointRadiusUnloadAvg = mid(params.amplitude) ?? liftingPointRadiusUnloadAvg
  }
}

// 打开塔吊 3D 展示并启动动画
function setupTower3D(deviceId) {
  showTC = true
  nextTick(() => {
    const container = threeContainer.value
    if (container && renderer && renderer.domElement.parentElement !== container) {
      container.appendChild(renderer.domElement)
    }
    if (deviceId === 1) {
      // 作业动画：从起始参数运动到结束参数
      animationFrame = 0
    } else if (deviceId === 2) {
      // 实时数据：跳转到最后一帧，仅绘制终态
      animationFrame = 600
    }
    syncFromParams()
    animate()
  })
}

// 关闭配置弹窗（停止动画并清理场景）
function closeConfigModal() {
  showTC = false
  cancelAnimationFrame(animId)
  animationFrame = 601
  if (renderer && renderer.domElement) renderer.domElement.remove()
  currentEditingDevice.value = null
  configModalVisible.value = false
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
  // 塔吊类型时，在弹窗右侧渲染 3D 模型
  if (deviceId === 1 || deviceId === 2) {
    setupTower3D(deviceId)
  }
}

// 监听配置参数变化，实时驱动 3D 动画（塔吊作业/实时数据）
watch(
  () => configForm.params,
  () => {
    const id = currentEditingDevice.value
    if (id !== 1 && id !== 2) return
    syncFromParams()
    if (id === 1) {
      animationFrame = 0 // 从起始帧重新播放
    } else {
      animationFrame = 600 // 仅绘制终态
    }
  },
  { deep: true }
)

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
    closeConfigModal()
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
        closeConfigModal()
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
  init3D()
})

onUnmounted(() => {
  showTC = false
  cancelAnimationFrame(animId)
  socket.value?.disconnect()
  renderer?.dispose()
})
</script>

<template>
  <div class="simulation-page">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="page-heading">
        <h2>设备通信模拟控制</h2>
        <span class="page-subtitle">设备数据模拟 · 塔吊 3D 实时联动</span>
      </div>
      <div class="ws-status" :class="{ connected: socketConnected }">
        <span class="ws-dot"></span>
        <span class="ws-text">{{ socketConnected ? 'Socket 已连接' : 'Socket 未连接' }}</span>
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
      width="1160px"
      :close-on-click-modal="false"
    >
      <div class="modal-layout">
        <div class="modal-main">
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
        </div>
        <!-- 塔吊 3D 模型展示区（仅在塔吊设备时显示） -->
        <div v-if="isTower" class="modal-sidebar">
          <h3 class="config-section-title center">塔吊3D模型展示</h3>
          <div class="three-toolbar">
            <button
              class="mode-btn"
              :class="{ active: !dragMode }"
              @click="setDragMode(false)"
            >旋转</button>
            <button
              class="mode-btn"
              :class="{ active: dragMode }"
              @click="setDragMode(true)"
            >拖动</button>
          </div>
          <div class="three-hint">{{ dragMode ? '按住左键拖动塔吊移动位置' : '按住左键旋转视角 · 滚轮缩放' }}</div>
          <div ref="threeContainer" class="three-container"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeConfigModal">取消</el-button>
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
  position: relative;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
  background:
    radial-gradient(900px 420px at 6% -8%, rgba(201, 162, 107, 0.16), transparent 60%),
    radial-gradient(800px 400px at 100% 0%, rgba(201, 162, 107, 0.10), transparent 55%),
    var(--bg-page);
}

/* ---------- 顶部标题 ---------- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  flex-shrink: 0;
}

.page-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-heading h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
  color: var(--text-strong);
}

.page-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.5px;
}

.ws-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border: 1px solid var(--border);
  background: var(--bg-white);
  border-radius: 30px;
  font-size: 12px;
  color: var(--text-muted);
  transition: border-color 0.3s, color 0.3s;
  box-shadow: var(--shadow-sm);
}

.ws-status.connected {
  border-color: rgba(77, 133, 71, 0.45);
  color: var(--success);
}

.ws-text {
  font-weight: 500;
}

.ws-dot {
  position: relative;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--text-faint);
  transition: background 0.3s;
}

.ws-status.connected .ws-dot {
  background: var(--success);
}

.ws-status.connected .ws-dot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(77, 133, 71, 0.4);
  animation: pulse-ring 1.8s ease-out infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}

/* ---------- 面板布局 ---------- */
.sim-main {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.panel {
  position: relative;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
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
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
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
  background: var(--primary-grad);
  border-radius: 2px;
}

.panel-count {
  font-size: 11px;
  color: var(--primary-text);
  padding: 2px 10px;
  border-radius: 12px;
  background: var(--primary-tint);
  border: 1px solid rgba(176, 123, 62, 0.25);
}

.panel-empty {
  color: var(--text-faint);
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
  width: 6px;
}

.status-list::-webkit-scrollbar-thumb,
.device-grid::-webkit-scrollbar-thumb,
.log-content::-webkit-scrollbar-thumb {
  background: rgba(176, 123, 62, 0.25);
  border-radius: 4px;
}

.status-item {
  position: relative;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid;
  transition: all 0.25s;
  overflow: hidden;
}

.status-item.connected {
  background: var(--success-tint);
  border-color: rgba(77, 133, 71, 0.35);
}

.status-item.connected::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--success);
}

.status-item.disconnected {
  background: var(--bg-white);
  border-color: var(--border);
}

.status-item-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.status-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
}

.status-item.connected .status-name {
  color: var(--text-main);
}

.status-name::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-faint);
}

.status-item.connected .status-name::before {
  background: var(--success);
  box-shadow: 0 0 6px rgba(77, 133, 71, 0.7);
}

.status-badge {
  align-self: flex-start;
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 10px;
  font-weight: 500;
}

.status-badge.on {
  background: var(--success-tint);
  color: var(--success);
}

.status-badge.off {
  background: var(--bg-muted);
  color: var(--text-muted);
}

.disconnect-btn {
  flex-shrink: 0;
  padding: 4px 10px;
  font-size: 11px;
  border: 1px solid rgba(194, 91, 91, 0.4);
  background: transparent;
  color: var(--danger);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.disconnect-btn:hover {
  background: var(--danger-tint);
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
  gap: 13px;
  padding: 14px 16px;
  border: 1px solid;
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: all 0.25s;
  min-width: 0;
}

.device-button.running {
  background: linear-gradient(135deg, var(--success-tint), rgba(77, 133, 71, 0.04));
  border-color: rgba(77, 133, 71, 0.4);
  color: var(--success);
}

.device-button.stopped {
  background: var(--bg-white);
  border-color: var(--border);
  color: var(--text-muted);
}

.device-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.device-button.stopped:hover {
  border-color: var(--border-strong);
  color: var(--text-main);
}

.device-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: var(--primary-tint);
  border: 1px solid rgba(176, 123, 62, 0.18);
  transition: all 0.25s;
}

.device-button.running .device-icon {
  background: var(--success-tint);
  border-color: rgba(77, 133, 71, 0.3);
}

.device-button-inner {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.device-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-button.stopped .device-name {
  color: var(--text-main);
}

.device-status-text {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.85;
}

.edit-btn {
  width: 56px;
  border: 1px solid rgba(176, 123, 62, 0.35);
  border-radius: 12px;
  background: var(--primary-tint);
  color: var(--primary-text);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.edit-btn:hover {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
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
  background: var(--bg-white);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.log-btn:hover {
  color: var(--text-main);
  border-color: var(--border-strong);
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: var(--bg-soft);
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.8;
}

.log-entry {
  display: flex;
  gap: 10px;
  word-break: break-all;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.15s;
}

.log-entry:hover {
  background: var(--bg-hover);
}

.log-time {
  color: var(--text-faint);
  flex-shrink: 0;
}

.log-message {
  color: var(--text-main);
}

.log-entry.sent .log-message {
  color: #2e7d4f;
}

.log-entry.received .log-message {
  color: #386fa4;
}

.log-entry.success .log-message {
  color: var(--success);
}

.log-entry.error .log-message {
  color: var(--danger);
}

.log-entry.warning .log-message {
  color: var(--warning);
}

.log-empty {
  color: var(--text-faint);
  text-align: center;
  padding: 30px 0;
}

/* ---------- 弹窗内部 ---------- */
.modal-layout {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.modal-main {
  flex: 1;
  min-width: 0;
}

.modal-sidebar {
  width: 520px;
  flex-shrink: 0;
  border-left: 1px solid var(--border);
  padding-left: 20px;
}

.three-container {
  width: 500px;
  height: 480px;
  background:
    radial-gradient(400px 300px at 50% 0%, rgba(201, 162, 107, 0.18), transparent 72%),
    var(--bg-soft);
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: inset 0 0 30px rgba(176, 123, 62, 0.08), var(--shadow-sm);
}

.three-container canvas {
  display: block;
}

.config-section-title.center {
  text-align: center;
  border-left: none;
  padding-left: 0;
}

.three-hint {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
  margin: 0 0 8px;
}

.three-toolbar {
  display: flex;
  gap: 6px;
  justify-content: center;
  margin-bottom: 8px;
}

.mode-btn {
  min-width: 84px;
  padding: 7px 0;
  font-size: 13px;
  border-radius: 8px;
  border: 1px solid var(--border-strong);
  background: var(--bg-white);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.mode-btn.active {
  background: var(--primary-grad);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 10px rgba(176, 123, 62, 0.25);
}

.config-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-text);
  margin: 6px 0 14px;
  padding-left: 10px;
  border-left: 3px solid var(--primary);
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-input {
  width: 165px;
}

.range-sep {
  color: var(--text-muted);
}

.confirm-text {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.8;
}

.confirm-text .highlight {
  color: var(--danger);
  font-weight: 600;
}

/* ---------- Element Plus 浅色适配 ---------- */
:deep(.el-dialog) {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border);
  margin-right: 0;
  padding: 16px 20px;
  background: var(--bg-soft);
}

:deep(.el-dialog__title) {
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px;
  color: var(--text-main);
}

:deep(.el-dialog__footer) {
  border-top: 1px solid var(--border);
  padding: 12px 20px;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-size: 13px;
}

:deep(.el-input__wrapper),
:deep(.el-input-number) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: none;
  border-radius: 8px;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input-number:hover) {
  border-color: var(--border-strong);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(176, 123, 62, 0.12);
}

:deep(.el-input__inner) {
  color: var(--text-main);
}

:deep(.el-select__wrapper) {
  background: var(--bg-soft);
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
}

:deep(.el-select__placeholder),
:deep(.el-select__selected-item) {
  color: var(--text-main);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary);
}

:deep(.el-button--primary:hover) {
  --el-button-bg-color: var(--el-color-primary-dark-2);
  --el-button-border-color: var(--el-color-primary-dark-2);
}

:deep(.el-button--danger) {
  --el-button-bg-color: var(--danger);
  --el-button-border-color: var(--danger);
}

:deep(.el-button--danger:hover) {
  --el-button-bg-color: #a84444;
  --el-button-border-color: #a84444;
}

:deep(.el-button) {
  --el-button-bg-color: var(--bg-white);
  --el-button-border-color: var(--border-strong);
  --el-button-text-color: var(--text-secondary);
  border-radius: 8px;
}

:deep(.el-button:hover) {
  --el-button-border-color: var(--primary);
  --el-button-text-color: var(--primary);
}

/* 下拉选项弹层（teleport 到 body，需全局样式） */
:global(.el-select-dropdown) {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 8px;
}

:global(.el-select-dropdown__item) {
  color: var(--text-main);
}

:global(.el-select-dropdown__item.is-hovering) {
  background: var(--bg-hover);
}

:global(.el-select-dropdown__item.is-selected) {
  color: var(--primary);
}

/* ---------- 响应式 ---------- */
@media (max-width: 1200px) {
  .status-panel {
    width: 280px;
  }

  .modal-sidebar {
    width: 480px;
  }

  .three-container {
    width: 460px;
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

  .modal-layout {
    flex-direction: column;
  }

  .modal-sidebar {
    width: 100%;
    border-left: none;
    border-top: 1px solid var(--border);
    padding-left: 0;
    padding-top: 16px;
  }

  .three-container {
    width: 100%;
  }
}
</style>

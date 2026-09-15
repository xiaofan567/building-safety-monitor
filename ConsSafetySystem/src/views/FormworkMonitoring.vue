<template>
  <div class="fw-wrap">
    <div class="fw-head">
      <h2 class="fw-title"> 施工设备 > 高支模监测分析 </h2>
    </div>

    <div class="fw-body">
      <!-- 左栏：实时监测数据 / 沉降数据 -->
      <div class="fw-col fw-col-left">
        <div class="panel fw-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时监测数据
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>监测项</th>
                <th>采集值</th>
                <th>单位</th>
                <th>监测时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in currentMonitorData" :key="item.name">
                <td>{{ item.name }}</td>
                <td :class="{'val-warn': item.status === 'warn', 'val-danger': item.status === 'danger'}">{{ item.value }}</td>
                <td>{{ item.unit }}</td>
                <td>{{ item.time || '--' }}</td>
              </tr>
              <tr v-if="currentMonitorData.length === 0">
                <td colspan="4" class="empty-cell">暂无监测数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="panel fw-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            沉降数据
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>监测时间</th>
                <th>沉降量(mm)</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in settlementData" :key="i">
                <td>{{ s.time }}</td>
                <td :class="{'val-warn': s.status === 'warn', 'val-danger': s.status === 'danger'}">{{ s.value }}</td>
                <td>
                  <span class="st-tag" :class="s.status">{{ s.statusText }}</span>
                </td>
              </tr>
              <tr v-if="settlementData.length === 0">
                <td colspan="3" class="empty-cell">暂无沉降数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 中栏：3D 模型 + 设备切换 + 图例 -->
      <div class="fw-col fw-col-center">
        <div class="fw-tabs">
          <div
            v-for="(item, index) in highFormworkList"
            :key="item.assetId"
            class="fw-tab"
            :class="{ active: activeTab === index }"
            @click="switchModel(index)"
          >
            {{ item.assetName || `高支模${index + 1}` }}
          </div>
        </div>

        <div class="panel fw-panel fw-3d-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            3D 结构状态
          </div>
          <div ref="modelContainer" class="model-container"></div>
          <div class="fw-legend">
            <span class="lg-item"><i class="dot normal"></i>正常</span>
            <span class="lg-item"><i class="dot warn"></i>预警</span>
            <span class="lg-item"><i class="dot danger"></i>报警</span>
          </div>
        </div>
      </div>

      <!-- 右栏：预警报警趋势 / 报警类型占比 -->
      <div class="fw-col fw-col-right">
        <div class="panel fw-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            预警报警趋势
          </div>
          <div ref="trendChartRef" class="chart chart-trend"></div>
        </div>

        <div class="panel fw-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            报警类型占比
          </div>
          <div ref="pieChartRef" class="chart chart-pie"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import * as echarts from 'echarts'
import { hfList, hfMonitor, hfAlarm } from '@/api/hfDfp'

const props = defineProps({
  initialAssetId: { type: Number, default: null }
})

// ---------- 数据 ----------
const highFormworkList = ref([])
const activeTab = ref(0)
const currentMonitorData = ref([])
const settlementData = ref([])
const currentAssetId = ref(null)
const monitorMap = ref({})

// ---------- 阈值（固定值，参考任务文档） ----------
const THRESHOLD_MAP = {
  '轴力': 50,
  '拉力': 50,
  '倾角': 1,
  '位移': 3,
  '沉降': 3,
  '电量': 20
}
function getWarningThreshold(type) {
  let t = 0
  for (const k in THRESHOLD_MAP) {
    if (type && type.includes(k)) { t = THRESHOLD_MAP[k]; break }
  }
  return t
}
function getAlarmThreshold(type) {
  const warn = getWarningThreshold(type)
  /** 无关键词时使用倾角/位移/沉降默认报警阈值 */
  if (type.includes('倾角')) return 2
  if (type.includes('沉降')) return 4
  if (type.includes('位移')) return 4
  if (type.includes('轴力') || type.includes('拉力')) return 70
  return warn ? warn * 1.4 : 0
}

/** 计算监测值状态：normal / warn / danger */
function calcStatus(name, value) {
  const v = Number(value)
  if (Number.isNaN(v)) return 'normal'
  const warn = getWarningThreshold(name)
  const alarm = getAlarmThreshold(name)
  if (alarm && v > alarm) return 'danger'
  if (warn && v > warn) return 'warn'
  return 'normal'
}

/** 沉降状态：≤2 正常，2-3 预警，>3 报警 */
function getSettlementStatus(value) {
  const v = Number(value)
  if (v <= 2) return 'normal'
  if (v <= 3) return 'warn'
  return 'danger'
}

// ---------- 图表 ----------
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

const chartColors = {
  red: '#e54545',
  yellow: '#f5a623',
  blue: '#b07b3e',
  gridLine: 'rgba(122,106,85,0.18)',
  textDim: '#9a8f7f'
}
const fmtDay = (d) => (d ? String(d).slice(5, 10) : '')

function areaOption(name, color, cats, data) {
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    grid: { top: 24, right: 18, bottom: 26, left: 44 },
    xAxis: {
      type: 'category',
      data: cats,
      boundaryGap: false,
      axisLine: { lineStyle: { color: chartColors.gridLine } },
      axisTick: { show: false },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    series: [{
      name, type: 'line', data, smooth: true, symbol: 'none',
      lineStyle: { color, width: 2 }, itemStyle: { color },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: color + '30' }, { offset: 1, color: color + '00' }
      ]) }
    }]
  }
}

function renderCharts(alarm) {
  if (!trendChart || !pieChart) return
  // 7 日趋势（取报警/预警统计末尾 7 天）
  const alarmDays = (alarm.alarmCountList || []).slice(-7).map((d) => d.alarmDate)
  const warnMap = {}
  ;(alarm.warnCountList || []).forEach((d) => { warnMap[d.alarmDate] = d.alarmCount })
  const cats = alarmDays.map(fmtDay)
  const alarmData = (alarm.alarmCountList || []).slice(-7).map((d) => d.alarmCount)
  const warnData = cats.map((_, i) => warnMap[alarmDays[i]] ?? null)
  trendChart.setOption(areaOption('报警', chartColors.red, cats, alarmData), true)
  trendChart.setOption({
    legend: { top: 0, right: 8, textStyle: { color: chartColors.textDim, fontSize: 11 }, itemWidth: 12, itemHeight: 6 },
    series: [
      areaOption('报警', chartColors.red, cats, alarmData).series[0],
      areaOption('预警', chartColors.yellow, cats, warnData).series[0]
    ]
  })

  const pieData = (alarm.alarmTypeList || []).map((t) => ({ name: t.alarmType, value: t.count }))
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: 'rgba(255,255,255,0.98)', borderColor: 'rgba(176,123,62,0.35)', textStyle: { color: '#332e26' } },
    legend: { orient: 'vertical', right: 6, top: 'center', textStyle: { color: chartColors.textDim, fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    color: ['#b07b3e', '#e54545', '#f5a623', '#5a8fd4', '#0d9e6c'],
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['38%', '50%'],
      itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 1 },
      label: { formatter: '{b}: {c}', color: '#8a7f6d', fontSize: 10 },
      data: pieData.length ? pieData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e7e2d8' } }]
    }]
  })
}

// ---------- 3D ----------
const modelContainer = ref(null)
let scene = null
let camera = null
let renderer = null
let labelRenderer = null
let controls = null
let formworkGroup = null
let rafId = null
let resizeHandler = null

function disposeObject(obj) {
  if (obj.geometry) obj.geometry.dispose()
  if (obj.material) {
    if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose())
    else obj.material.dispose()
  }
}

function addLights() {
  scene.children = scene.children.filter((c) => !(c instanceof THREE.Light))
  scene.add(new THREE.AmbientLight(0xffffff, 0.55))
  const main = new THREE.DirectionalLight(0xffffff, 1)
  main.position.set(12, 18, 8)
  main.castShadow = true
  main.shadow.mapSize.set(1024, 1024)
  scene.add(main)
  const fill = new THREE.DirectionalLight(0xffffff, 0.45)
  fill.position.set(-10, 6, -12)
  scene.add(fill)
  scene.add(new THREE.HemisphereLight(0xffffff, 0x8a8a8a, 0.5))
}

const STATUS_COLORS = { normal: 0x3f8fb3, warn: 0xe0a83c, danger: 0xd9565f }

function buildFormworkModel(monitorData) {
  // 清理旧模型与旧标签
  while (formworkGroup.children.length > 0) {
    const child = formworkGroup.children[0]
    formworkGroup.remove(child)
    disposeObject(child)
  }
  scene.children = scene.children.filter((c) => !c.isCSS2DObject)

  const names = Object.keys(monitorData || {})
  const n = names.length
  const gridSize = n ? Math.max(3, Math.ceil(Math.sqrt(n))) : 4
  const totalH = 6 // 塔架总高
  const span = 2.6 // 跨距
  const colSize = 0.34
  const beamSize = 0.26
  const extent = (gridSize - 1) * span

  const steelMat = new THREE.MeshStandardMaterial({ color: 0x8d8d94, metalness: 0.6, roughness: 0.35 })
  const steelTop = new THREE.MeshStandardMaterial({ color: 0x6e6e75, metalness: 0.65, roughness: 0.35, transparent: true, opacity: 0.28, depthWrite: false })
  // 顶部面板改为极淡的线框，不遮挡下方结构
  const deckMat = new THREE.MeshBasicMaterial({ color: 0x9fb8c9, transparent: true, opacity: 0.16, depthWrite: false, side: THREE.DoubleSide })

  // 立柱 + 柱顶状态灯 + 测点标签
  names.forEach((name, k) => {
    const vals = monitorData[name] || []
    const latest = vals[vals.length - 1] || {}
    const status = calcStatus(name, latest.monitorData)
    const i = Math.floor(k / gridSize)
    const j = k % gridSize
    const x = (i - (gridSize - 1) / 2) * span
    const z = (j - (gridSize - 1) / 2) * span

    const col = new THREE.Mesh(
      new THREE.BoxGeometry(colSize, totalH, colSize),
      new THREE.MeshStandardMaterial({ color: STATUS_COLORS[status], metalness: 0.25, roughness: 0.55 })
    )
    col.position.set(x, totalH / 2, z)
    col.castShadow = true
    formworkGroup.add(col)

    // 柱顶状态灯（发光球体）
    const lamp = new THREE.Mesh(
      new THREE.SphereGeometry(0.22, 20, 20),
      new THREE.MeshStandardMaterial({ color: STATUS_COLORS[status], emissive: STATUS_COLORS[status], emissiveIntensity: 0.9 })
    )
    lamp.position.set(x, totalH + 0.22, z)
    formworkGroup.add(lamp)

    // 测点标签
    const labelDiv = document.createElement('div')
    labelDiv.className = 'data-label'
    labelDiv.style.setProperty('--st-color', '#' + STATUS_COLORS[status].toString(16).padStart(6, '0'))
    labelDiv.innerHTML = `<div class="dl-name">${name}</div><div class="dl-val">${latest.unit ? latest.monitorData + ' ' + latest.unit : (latest.monitorData ?? '--')}</div>`
    const label = new CSS2DObject(labelDiv)
    label.position.set(x, totalH + 0.75, z)
    scene.add(label)
  })

  // 水平横梁：多层，每层在纵横方向拉通
  const levels = [totalH, totalH * 0.66, totalH * 0.33]
  for (let li = 0; li < levels.length; li++) {
    const y = levels[li]
    const m = li === 0 ? steelTop : steelMat
    for (let g = 0; g < gridSize; g++) {
      const zc = (g - (gridSize - 1) / 2) * span
      const bx = new THREE.Mesh(new THREE.BoxGeometry(extent, beamSize, beamSize), m)
      bx.position.set(0, y, zc); bx.castShadow = true; formworkGroup.add(bx)

      const xr = (g - (gridSize - 1) / 2) * span
      const bz = new THREE.Mesh(new THREE.BoxGeometry(beamSize, beamSize, extent), m)
      bz.position.set(xr, y, 0); bz.castShadow = true; formworkGroup.add(bz)
    }
  }

  // 顶部现浇面板
  const deck = new THREE.Mesh(new THREE.BoxGeometry(extent + span * 0.5, 0.3, extent + span * 0.5), deckMat)
  deck.position.y = totalH + 0.35
  deck.castShadow = deck.receiveShadow = true
  formworkGroup.add(deck)
}

function buildGround() {
  const g = new THREE.Mesh(new THREE.PlaneGeometry(26, 26), new THREE.MeshStandardMaterial({ color: 0xedeae4, roughness: 0.95 }))
  g.rotation.x = -Math.PI / 2
  g.position.y = -0.02
  g.receiveShadow = true
  scene.add(g)
  const grid = new THREE.GridHelper(26, 13, 0xcfc9bf, 0xe5e0d6)
  grid.position.y = 0
  scene.add(grid)
}

// ---------- 数据加载 ----------
function sortByTime(list) {
  return [...(list || [])].sort((a, b) => new Date(a.monitorTime) - new Date(b.monitorTime))
}

async function loadHighFormworkData(assetId) {
  if (!assetId) return
  const [monitorRes, alarmRes] = await Promise.all([hfMonitor(assetId), hfAlarm(assetId)])
  const monitor = (monitorRes && monitorRes.data) || {}
  monitorMap.value = monitor

  // 实时监测数据表（每个监测点取最新值）
  const rows = []
  Object.keys(monitor).forEach((name) => {
    const list = sortByTime(monitor[name])
    if (!list.length) return
    const latest = list[list.length - 1]
    rows.push({
      name,
      value: latest.monitorData ?? '--',
      unit: latest.unit || '',
      time: latest.monitorTime ? String(latest.monitorTime).replace('T', ' ').slice(0, 19) : '',
      status: calcStatus(name, latest.monitorData)
    })
  })
  currentMonitorData.value = rows

  // 沉降数据表（所有含“沉降”的点合并，按时间排序）
  const settleRows = []
  Object.keys(monitor).forEach((name) => {
    if (!name.includes('沉降')) return
    sortByTime(monitor[name]).forEach((v) => {
      const value = Number(v.monitorData)
      const status = getSettlementStatus(value)
      settleRows.push({
        time: v.monitorTime ? String(v.monitorTime).replace('T', ' ').slice(5, 16) : '',
        value: Number.isNaN(value) ? v.monitorData : value,
        status,
        statusText: status === 'normal' ? '正常' : status === 'warn' ? '预警' : '报警'
      })
    })
  })
  settlementData.value = settleRows

  // 3D 模型
  buildFormworkModel(monitor)
  // 图表
  const alarm = (alarmRes && alarmRes.data) || {}
  renderCharts(alarm)
}

function switchModel(index) {
  activeTab.value = index
  const item = highFormworkList.value[index]
  if (!item) return
  currentAssetId.value = item.assetId
  loadHighFormworkData(item.assetId)
}

async function init() {
  const res = await hfList()
  const list = (res && res.data) || []
  highFormworkList.value = list
  if (list.length) {
    let idx = 0
    const preset = props.initialAssetId
    if (preset) {
      const found = list.findIndex((t) => t.assetId == preset)
      if (found >= 0) idx = found
    }
    activeTab.value = idx
    currentAssetId.value = list[idx].assetId
    await loadHighFormworkData(list[idx].assetId)
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  nextTick(() => {
    if (!modelContainer.value) return
    const w = modelContainer.value.clientWidth
    const h = modelContainer.value.clientHeight
    scene = new THREE.Scene()
    scene.background = new THREE.Color(0xe8edf2)
    camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 1000)
    camera.position.set(10, 7.5, 10)
    camera.lookAt(0, 3, 0)

    renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setSize(w, h)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFSoftShadowMap
    renderer.domElement.style.zIndex = '1'
    modelContainer.value.appendChild(renderer.domElement)

    labelRenderer = new CSS2DRenderer()
    labelRenderer.setSize(w, h)
    labelRenderer.domElement.style.position = 'absolute'
    labelRenderer.domElement.style.top = '0'
    labelRenderer.domElement.style.left = '0'
    labelRenderer.domElement.style.pointerEvents = 'none'
    labelRenderer.domElement.style.zIndex = '2'
    modelContainer.value.appendChild(labelRenderer.domElement)

    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.1
    controls.target.set(0, 2.8, 0)
    controls.minDistance = 5
    controls.maxDistance = 30
    controls.update()

    formworkGroup = new THREE.Group()
    scene.add(formworkGroup)
    addLights()
    buildGround()
    animate()
    init()
  })

  timer = setInterval(() => {
    if (currentAssetId.value) loadHighFormworkData(currentAssetId.value)
  }, 10000)

  resizeHandler = () => {
    if (!renderer || !scene || !camera) return
    const w = modelContainer.value.clientWidth
    const h = modelContainer.value.clientHeight
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
    labelRenderer && labelRenderer.setSize(w, h)
    trendChart && trendChart.resize()
    pieChart && pieChart.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

function animate() {
  rafId = requestAnimationFrame(animate)
  if (controls) controls.update()
  if (renderer && scene && camera) renderer.render(scene, camera)
  if (labelRenderer && scene && camera) labelRenderer.render(scene, camera)
}

let timer = null

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (rafId) cancelAnimationFrame(rafId)
  trendChart && trendChart.dispose()
  pieChart && pieChart.dispose()
  while (formworkGroup && formworkGroup.children.length > 0) {
    const child = formworkGroup.children[0]
    formworkGroup.remove(child)
    disposeObject(child)
  }
  renderer && renderer.dispose()
  renderer && renderer.domElement && renderer.domElement.remove()
})
</script>

<style scoped>
.fw-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}
.fw-head { padding: 6px 4px 10px; }
.fw-title { font-size: 15px; font-weight: 600; color: var(--text-muted); }
.fw-body {
  flex: 1;
  display: flex;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}
.fw-col {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
.fw-col-left { flex: 1 1 0; }
.fw-col-center { flex: 3 1 0; }
.fw-col-right { flex: 1 1 0; }

.panel {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
}
.fw-panel { padding: 12px; flex-shrink: 0; }
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
  margin-bottom: 10px;
}
.panel-title-icon { width: 20px; height: 20px; }

/* 表格 */
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.data-table th {
  text-align: left;
  padding: 6px 8px;
  color: var(--text-secondary);
  background: var(--bg-soft);
  font-weight: 500;
}
.data-table td { padding: 6px 8px; color: var(--text-main); }
.data-table tbody tr:nth-child(even) { background: var(--bg-muted); }
.val-warn { color: #f5a623; font-weight: 600; }
.val-danger { color: var(--danger); font-weight: 600; }
.empty-cell { color: var(--text-faint); text-align: center; padding: 14px 0; }

/* 沉降状态 */
.st-tag { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px; }
.st-tag.normal { color: var(--success); background: rgba(13, 158, 108, 0.12); }
.st-tag.warn { color: #f5a623; background: rgba(245, 166, 35, 0.15); }
.st-tag.danger { color: var(--danger); background: rgba(229, 69, 69, 0.12); }

/* 切换 */
.fw-tabs { display: flex; flex-wrap: wrap; gap: 8px; padding-bottom: 2px; }
.fw-tab {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-white);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}
.fw-tab.active { color: #fff; background: var(--primary-grad); border-color: transparent; }

/* 3D 面板 */
.fw-3d-panel { flex: 1; display: flex; flex-direction: column; min-height: 360px; }
.model-container {
  flex: 1;
  min-height: 300px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.fw-legend { display: flex; gap: 16px; margin-top: 8px; justify-content: center; }
.lg-item { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: var(--text-secondary); }
.lg-item .dot { width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.dot.normal { background: #3f8fb3; }
.dot.warn { background: #e0a83c; }
.dot.danger { background: #d9565f; }

.chart-trend { height: 220px; width: 100%; }
.chart-pie { height: 240px; width: 100%; }
</style>

<style>
/* 全局样式（3D 标签，避免 scoped 覆盖） */
.data-label {
  position: relative;
  padding: 3px 7px;
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid var(--st-color, #3f8fb3);
  border-radius: 5px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
  font-size: 10px;
  font-family: inherit;
  line-height: 1.4;
  color: #332e26;
  white-space: nowrap;
  text-align: center;
  transform: translate(-50%, -100%);
  pointer-events: none;
}
.data-label::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -5px;
  transform: translateX(-50%);
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid var(--st-color, #3f8fb3);
}
.data-label .dl-name { font-weight: 600; font-size: 10px; }
.data-label .dl-val { color: #5a5244; font-size: 10px; }
</style>
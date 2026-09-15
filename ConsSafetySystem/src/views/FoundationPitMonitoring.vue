<template>
  <div class="fp-wrap">
    <div class="fp-head">
      <h2 class="fp-title"> 施工设备 > 深基坑监测分析 </h2>
    </div>

    <div class="fp-body">
      <!-- 左栏：实时监测数据 / 沉降数据 -->
      <div class="fp-col fp-col-left">
        <div class="panel fp-panel">
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

        <div class="panel fp-panel">
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

      <!-- 中栏：3D 基坑模型 + 设备切换 + 图例 -->
      <div class="fp-col fp-col-center">
        <div class="fp-tabs">
          <div
            v-for="(item, index) in foundationList"
            :key="item.assetId"
            class="fp-tab"
            :class="{ active: activeTab === index }"
            @click="switchModel(index)"
          >
            {{ item.assetName || `深基坑${index + 1}` }}
          </div>
        </div>

        <div class="panel fp-panel fp-3d-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            3D 基坑结构状态
          </div>
          <div ref="modelContainer" class="model-container"></div>
          <div class="fp-legend">
            <span class="lg-item"><i class="dot normal"></i>正常</span>
            <span class="lg-item"><i class="dot warn"></i>预警</span>
            <span class="lg-item"><i class="dot danger"></i>报警</span>
          </div>
        </div>
      </div>

      <!-- 右栏：预警报警趋势 / 报警类型占比 -->
      <div class="fp-col fp-col-right">
        <div class="panel fp-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            预警报警趋势
          </div>
          <div ref="trendChartRef" class="chart chart-trend"></div>
        </div>

        <div class="panel fp-panel">
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import * as echarts from 'echarts'
import { dfpList, hfMonitor, hfAlarm } from '@/api/hfDfp'

const props = defineProps({
  initialAssetId: { type: Number, default: null }
})

// ---------- 数据 ----------
const foundationList = ref([])
const activeTab = ref(0)
const currentMonitorData = ref([])
const settlementData = ref([])
const currentAssetId = ref(null)

// ---------- 阈值 ----------
const THRESHOLD_MAP = {
  '轴力': 50,
  '拉力': 50,
  '倾角': 1,
  '位移': 3,
  '沉降': 3,
  '水位': 5,
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
  if (type.includes('倾角')) return 2
  if (type.includes('沉降')) return 4
  if (type.includes('位移')) return 4
  if (type.includes('轴力') || type.includes('拉力')) return 70
  if (type.includes('水位')) return 8
  return warn ? warn * 1.4 : 0
}
function calcStatus(name, value) {
  const v = Number(value)
  if (Number.isNaN(v)) return 'normal'
  const warn = getWarningThreshold(name)
  const alarm = getAlarmThreshold(name)
  if (alarm && v > alarm) return 'danger'
  if (warn && v > warn) return 'warn'
  return 'normal'
}
const SEVERITY = { normal: 0, warn: 1, danger: 2 }
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
  red: '#e54545', yellow: '#f5a623', gridLine: 'rgba(122,106,85,0.18)', textDim: '#9a8f7f'
}
const fmtDay = (d) => (d ? String(d).slice(5, 10) : '')
function areaLine(name, color, cats, data) {
  return {
    name, type: 'line', data, smooth: true, symbol: 'none', lineStyle: { color, width: 2 },
    itemStyle: { color },
    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: color + '30' }, { offset: 1, color: color + '00' }
    ]) }
  }
}
function renderCharts(alarm) {
  if (!trendChart || !pieChart) return
  const alarmDays = (alarm.alarmCountList || []).slice(-7).map((d) => d.alarmDate)
  const warnMap = {}
  ;(alarm.warnCountList || []).forEach((d) => { warnMap[d.alarmDate] = d.alarmCount })
  const cats = alarmDays.map(fmtDay)
  const alarmData = (alarm.alarmCountList || []).slice(-7).map((d) => d.alarmCount)
  const warnData = cats.map((_, i) => warnMap[alarmDays[i]] ?? null)
  trendChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.98)', borderColor: 'rgba(176,123,62,0.35)', textStyle: { color: '#332e26', fontSize: 12 } },
    legend: { top: 0, right: 8, textStyle: { color: chartColors.textDim, fontSize: 11 }, itemWidth: 12, itemHeight: 6 },
    grid: { top: 28, right: 18, bottom: 26, left: 44 },
    xAxis: { type: 'category', data: cats, boundaryGap: false, axisLine: { lineStyle: { color: chartColors.gridLine } }, axisTick: { show: false }, axisLabel: { color: chartColors.textDim, fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } }, axisLabel: { color: chartColors.textDim, fontSize: 10 } },
    series: [areaLine('报警', chartColors.red, cats, alarmData), areaLine('预警', chartColors.yellow, cats, warnData)]
  }, true)

  const pieData = (alarm.alarmTypeList || []).map((t) => ({ name: t.alarmType, value: t.count }))
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: 'rgba(255,255,255,0.98)', borderColor: 'rgba(176,123,62,0.35)', textStyle: { color: '#332e26' } },
    legend: { orient: 'vertical', right: 6, top: 'center', textStyle: { color: chartColors.textDim, fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    color: ['#b07b3e', '#e54545', '#f5a623', '#5a8fd4', '#0d9e6c'],
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['38%', '50%'], itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 1 },
      label: { formatter: '{b}: {c}', color: '#8a7f6d', fontSize: 10 },
      data: pieData.length ? pieData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e7e2d8' } }]
    }]
  }, true)
}

// ---------- 3D ----------
const modelContainer = ref(null)
let scene = null
let camera = null
let renderer = null
let labelRenderer = null
let controls = null
let foundationGroup = null
let rafId = null
let resizeHandler = null

const STATUS_COLORS = { normal: 0x3f8fb3, warn: 0xe0a83c, danger: 0xd9565f }

function disposeObject(obj) {
  if (obj.geometry) obj.geometry.dispose()
  if (obj.material) {
    if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose())
    else obj.material.dispose()
  }
}

function addLights() {
  scene.children = scene.children.filter((c) => !(c instanceof THREE.Light))
  scene.add(new THREE.AmbientLight(0xffffff, 0.6))
  const main = new THREE.DirectionalLight(0xffffff, 1)
  main.position.set(14, 20, 10)
  main.castShadow = true
  main.shadow.mapSize.set(1024, 1024)
  scene.add(main)
  const fill = new THREE.DirectionalLight(0xffffff, 0.45)
  fill.position.set(-10, 6, -12)
  scene.add(fill)
  scene.add(new THREE.HemisphereLight(0xffffff, 0x8a8a8a, 0.5))
}

/** 生成壁面网格线几何（LineSegments） */
function createGridGeometry(w, h, gridSize, thickness = 0) {
  const positions = []
  for (let i = 0; i <= gridSize; i++) {
    const u = (i / gridSize) * w - w / 2
    positions.push(u, -h / 2, 0, u, h / 2, 0)
    const v = (i / gridSize) * h - h / 2
    positions.push(-w / 2, v, 0, w / 2, v, 0)
  }
  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
  return geo
}

function topSeverity(list) {
  const vals = list || []
  return vals.reduce((acc, s) => Math.max(acc, SEVERITY[s] || 0), 0)
}
function colorForSev(sev) {
  if (sev >= 2) return STATUS_COLORS.danger
  if (sev === 1) return STATUS_COLORS.warn
  return 0x9aa0a6
}

function buildFoundationModel(monitorData) {
  // 清理旧模型与标签
  while (foundationGroup.children.length > 0) {
    const child = foundationGroup.children[0]
    foundationGroup.remove(child)
    disposeObject(child)
  }
  scene.children = scene.children.filter((c) => !c.isCSS2DObject)

  // 略：基坑尺寸/支撑层数（后端无尺寸字段，使用默认参数，后续可替换）
  const length = 12
  const width = 8
  const depth = 6
  const supportLayers = 2
  const supportSpacing = 2
  const wallThickness = 0.15
  const gridSize = 5

  const names = Object.keys(monitorData || {})
  const n = names.length
  const pointStatus = names.map((name) => {
    const vals = monitorData[name] || []
    const latest = vals[vals.length - 1] || {}
    return { name, status: calcStatus(name, latest.monitorData), unit: latest.unit, value: latest.monitorData }
  })

  // 底部
  const bottomMat = new THREE.MeshStandardMaterial({ color: 0x696969, roughness: 0.9 })
  const bottom = new THREE.Mesh(new THREE.BoxGeometry(length - 0.5, wallThickness, width - 0.5), bottomMat)
  bottom.position.y = -depth
  bottom.receiveShadow = true
  foundationGroup.add(bottom)

  // 支撑格栅
  const supportMat = new THREE.MeshStandardMaterial({ color: 0x7B1FA2, metalness: 0.2, roughness: 0.6 })
  const supportWarn = new THREE.MeshStandardMaterial({ color: STATUS_COLORS.warn, metalness: 0.2, roughness: 0.6 })
  const supportDanger = new THREE.MeshStandardMaterial({ color: STATUS_COLORS.danger, metalness: 0.2, roughness: 0.6 })
  const supportYStep = depth / (supportLayers + 1)
  for (let li = 0; li < supportLayers; li++) {
    const supportY = -depth + supportYStep * (li + 1)
    // 该层状态取监测点分组
    const idx = Math.min(li, n - 1)
    const sev = n ? SEVERITY[pointStatus[idx].status] : 0
    const mat = sev === 2 ? supportDanger : sev === 1 ? supportWarn : supportMat
    for (let x = -length / 2 + 0.5; x <= length / 2 - 0.3; x += supportSpacing) {
      const bar = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.22, width - 1), mat)
      bar.position.set(x, supportY, 0)
      bar.castShadow = true
      foundationGroup.add(bar)
    }
    for (let z = -width / 2 + 0.5; z <= width / 2 - 0.3; z += supportSpacing) {
      const bar = new THREE.Mesh(new THREE.BoxGeometry(length - 1, 0.22, 0.22), mat)
      bar.position.set(0, supportY, z)
      bar.castShadow = true
      foundationGroup.add(bar)
    }
  }

  // 四周格栅围护墙
  const wallDefs = [
    { w: length, pos: [0, -depth / 2, width / 2], rot: [0, 0, 0] },
    { w: length, pos: [0, -depth / 2, -width / 2], rot: [0, 0, 0] },
    { w: width, pos: [-length / 2, -depth / 2, 0], rot: [0, Math.PI / 2, 0] },
    { w: width, pos: [length / 2, -depth / 2, 0], rot: [0, Math.PI / 2, 0] }
  ]
  // 墙状态：按监测点索引分 4 组取最高
  const groupCount = Math.max(1, Math.ceil(n / 4))
  for (let k = 0; k < 4; k++) {
    const group = pointStatus.slice(k * groupCount, (k + 1) * groupCount).map((p) => p.status)
    const sev = topSeverity(group)
    const d = wallDefs[k]
    const hgt = depth
    const geo = createGridGeometry(d.w, hgt, gridSize)
    const matLine = new THREE.LineBasicMaterial({ color: colorForSev(sev), transparent: true, opacity: 0.92 })
    const wall = new THREE.LineSegments(geo, matLine)
    wall.position.set(...d.pos)
    wall.rotation.set(...d.rot)
    foundationGroup.add(wall)
  }

  // 监测点：沿顶部周界分布，颜色 + 标签
  const halfL = length / 2
  const halfW = width / 2
  const perimeter = 2 * (length + width)
  pointStatus.forEach((p, k) => {
    const f = n > 1 ? k / n : 0
    const dist = f * perimeter
    let x, z
    if (dist < length) { x = -halfL + dist; z = halfW }
    else if (dist < length + width) { x = halfL; z = halfW - (dist - length) }
    else if (dist < 2 * length + width) { x = halfL - (dist - length - width); z = -halfW }
    else { x = -halfL; z = -halfW + (dist - 2 * length - width) }
    const y = 0.2
    const sev = SEVERITY[p.status]
    const lampMat = new THREE.MeshStandardMaterial({ color: STATUS_COLORS[p.status], emissive: STATUS_COLORS[p.status], emissiveIntensity: 0.9 })
    const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.18, 16, 16), lampMat)
    lamp.position.set(x, y, z)
    foundationGroup.add(lamp)

    const labelDiv = document.createElement('div')
    labelDiv.className = 'data-label'
    labelDiv.style.setProperty('--st-color', '#' + STATUS_COLORS[p.status].toString(16).padStart(6, '0'))
    labelDiv.innerHTML = `<div class="dl-name">${p.name}</div><div class="dl-val">${p.unit ? p.value + ' ' + p.unit : (p.value ?? '--')}</div>`
    const label = new CSS2DObject(labelDiv)
    label.position.set(x, 0.6, z)
    scene.add(label)
  })
}

function buildGround() {
  // 地面做成半透明，避免挡住下方的基坑
  const g = new THREE.Mesh(new THREE.PlaneGeometry(30, 30), new THREE.MeshStandardMaterial({ color: 0xbfd3dd, roughness: 0.95, transparent: true, opacity: 0.35, depthWrite: false }))
  g.rotation.x = -Math.PI / 2
  g.position.y = 0
  g.receiveShadow = true
  scene.add(g)
  const grid = new THREE.GridHelper(30, 15, 0x8fb0c2, 0xb7cdd8)
  grid.position.y = 0.01
  scene.add(grid)
}

// ---------- 数据加载 ----------
function sortByTime(list) {
  return [...(list || [])].sort((a, b) => new Date(a.monitorTime) - new Date(b.monitorTime))
}
async function loadFoundationData(assetId) {
  if (!assetId) return
  const [monitorRes, alarmRes] = await Promise.all([hfMonitor(assetId), hfAlarm(assetId)])
  const monitor = (monitorRes && monitorRes.data) || {}
  // 实时监测表
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
  // 沉降表
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
  // 3D + 图表
  buildFoundationModel(monitor)
  renderCharts((alarmRes && alarmRes.data) || {})
}

function switchModel(index) {
  activeTab.value = index
  const item = foundationList.value[index]
  if (!item) return
  currentAssetId.value = item.assetId
  loadFoundationData(item.assetId)
}

async function init() {
  const res = await dfpList()
  const list = (res && res.data) || []
  foundationList.value = list
  if (list.length) {
    let idx = 0
    if (props.initialAssetId) {
      const found = list.findIndex((t) => t.assetId == props.initialAssetId)
      if (found >= 0) idx = found
    }
    activeTab.value = idx
    currentAssetId.value = list[idx].assetId
    await loadFoundationData(list[idx].assetId)
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
    camera = new THREE.PerspectiveCamera(55, w / h, 0.1, 200)
    camera.position.set(28, 22, 28)
    camera.lookAt(0, 0, 0)

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
    controls.target.set(0, -3, 0)
    controls.minDistance = 8
    controls.maxDistance = 50
    controls.update()

    foundationGroup = new THREE.Group()
    scene.add(foundationGroup)
    addLights()
    buildGround()
    animate()
    init()
  })

  // 图表初始化
  trendChart = echarts.init(trendChartRef.value)
  pieChart = echarts.init(pieChartRef.value)

  timer = setInterval(() => {
    if (currentAssetId.value) loadFoundationData(currentAssetId.value)
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
  while (foundationGroup && foundationGroup.children.length > 0) {
    const child = foundationGroup.children[0]
    foundationGroup.remove(child)
    disposeObject(child)
  }
  renderer && renderer.dispose()
  renderer && renderer.domElement && renderer.domElement.remove()
})
</script>

<style scoped>
.fp-wrap { height: 100%; display: flex; flex-direction: column; background: transparent; }
.fp-head { padding: 6px 4px 10px; }
.fp-title { font-size: 15px; font-weight: 600; color: var(--text-muted); }
.fp-body { flex: 1; display: flex; gap: 14px; min-height: 0; overflow: hidden; }
.fp-col { height: 100%; display: flex; flex-direction: column; gap: 14px; min-height: 0; overflow-y: auto; overflow-x: hidden; }
.fp-col-left { flex: 1 1 0; }
.fp-col-center { flex: 3 1 0; }
.fp-col-right { flex: 1 1 0; }

.panel { background: var(--bg-white); border: 1px solid var(--border); border-radius: 10px; box-shadow: var(--shadow-sm); }
.fp-panel { padding: 12px; flex-shrink: 0; }
.panel-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: var(--text-strong); margin-bottom: 10px; }
.panel-title-icon { width: 20px; height: 20px; }

.data-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.data-table th { text-align: left; padding: 6px 8px; color: var(--text-secondary); background: var(--bg-soft); font-weight: 500; }
.data-table td { padding: 6px 8px; color: var(--text-main); }
.data-table tbody tr:nth-child(even) { background: var(--bg-muted); }
.val-warn { color: #f5a623; font-weight: 600; }
.val-danger { color: var(--danger); font-weight: 600; }
.empty-cell { color: var(--text-faint); text-align: center; padding: 14px 0; }

.st-tag { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px; }
.st-tag.normal { color: var(--success); background: rgba(13, 158, 108, 0.12); }
.st-tag.warn { color: #f5a623; background: rgba(245, 166, 35, 0.15); }
.st-tag.danger { color: var(--danger); background: rgba(229, 69, 69, 0.12); }

.fp-tabs { display: flex; flex-wrap: wrap; gap: 8px; padding-bottom: 2px; }
.fp-tab { padding: 6px 14px; border-radius: 8px; font-size: 13px; color: var(--text-secondary); background: var(--bg-white); border: 1px solid var(--border); cursor: pointer; transition: all 0.2s; }
.fp-tab.active { color: #fff; background: var(--primary-grad); border-color: transparent; }

.fp-3d-panel { flex: 1; display: flex; flex-direction: column; min-height: 360px; }
.model-container { flex: 1; min-height: 300px; position: relative; border-radius: 8px; overflow: hidden; background: #e8edf2; }
.fp-legend { display: flex; gap: 16px; margin-top: 8px; justify-content: center; }
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
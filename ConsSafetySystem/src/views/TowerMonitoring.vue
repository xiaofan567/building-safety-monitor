<template>
  <div class="tow-wrap">
    <div class="tow-head">
      <h2 class="tow-title"> 施工设备 > 塔机监测分析 </h2>
    </div>

    <div class="tow-body">
      <!-- 左栏：设备报警 / 报警趋势 / 设备信息 -->
      <div class="tow-col tow-col-left">
        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            设备报警信息
          </div>
          <div class="alarm-grid">
            <div class="alarm-cell">
              <div class="alarm-icon alarm-warn"><img src="../assets/img/yjimg.png" alt="预警" /></div>
              <div class="alarm-num">{{ alarmInfo.todayWarning }}</div>
              <div class="alarm-label">今日预警</div>
            </div>
            <div class="alarm-cell">
              <div class="alarm-icon alarm-warn"><img src="../assets/img/yjimg.png" alt="预警" /></div>
              <div class="alarm-num">{{ alarmInfo.totalWarning }}</div>
              <div class="alarm-label">累计预警</div>
            </div>
            <div class="alarm-cell">
              <div class="alarm-icon alarm-alarm"><img src="../assets/img/bjimg.png" alt="报警" /></div>
              <div class="alarm-num">{{ alarmInfo.todayAlarm }}</div>
              <div class="alarm-label">当日报警</div>
            </div>
            <div class="alarm-cell">
              <div class="alarm-icon alarm-alarm"><img src="../assets/img/bjimg.png" alt="报警" /></div>
              <div class="alarm-num">{{ alarmInfo.totalAlarm }}</div>
              <div class="alarm-label">累计报警</div>
            </div>
          </div>
        </div>

        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            报警趋势
          </div>
          <div class="range-switch">
            <span
              class="range-btn"
              :class="{ active: alarmRange === 30 }"
              @click="switchRange(30)"
            >近30日</span>
            <span
              class="range-btn"
              :class="{ active: alarmRange === 7 }"
              @click="switchRange(7)"
            >近7日</span>
          </div>
          <div ref="alarmChartRef" class="chart chart-trend"></div>
        </div>

        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            设备信息
          </div>
          <div class="info-table">
            <div class="info-row"><span class="info-key">设备名称</span><span class="info-val">{{ towerData.name }}</span></div>
            <div class="info-row"><span class="info-key">设备编号</span><span class="info-val">{{ towerData.assetCode }}</span></div>
            <div class="info-row"><span class="info-key">最大吊重</span><span class="info-val">{{ towerData.maxWeight }} t</span></div>
            <div class="info-row"><span class="info-key">额定力矩</span><span class="info-val">{{ towerData.ratedMoment }} M·T</span></div>
            <div class="info-row"><span class="info-key">前臂长</span><span class="info-val">{{ towerData.foreArmLength }} m</span></div>
            <div class="info-row"><span class="info-key">后臂长</span><span class="info-val">{{ towerData.rearArmLength }} m</span></div>
            <div class="info-row"><span class="info-key">最大吊高</span><span class="info-val">{{ towerData.maxHeight }} m</span></div>
            <div class="info-row"><span class="info-key">额定吊重</span><span class="info-val">{{ towerData.ratedLoad }} t</span></div>
          </div>
        </div>
      </div>

      <!-- 中栏：塔吊切换 / 设备信息与状态 / 实时工作数据 -->
      <div class="tow-col tow-col-center">
        <div class="tower-tabs">
          <div
            v-for="(item, index) in towerListData"
            :key="item.assetId"
            class="tower-tab"
            :class="{ active: activeTab === index }"
            @click="switchTower(index)"
          >
            <span class="status-dot" :class="onlineMap[item.assetId] ? 'on' : 'off'"></span>
            {{ item.assetName || `塔吊${index + 1}` }}
          </div>
        </div>

        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            设备信息与状态
          </div>
          <div class="status-grid">
            <div class="status-left">
              <div class="device-img-wrap">
                <img src="../assets/img/taji.png" alt="塔吊" class="device-img" />
              </div>
              <div class="device-no">编号：{{ towerData.assetCode }}</div>
              <div class="device-time">数据时间：{{ realtimeData.monitorTime }}</div>
            </div>

            <div class="status-mid">
              <div class="stat-card">
                <span class="stat-label">工作时长</span>
                <span class="stat-value">{{ workInfo.totalWorkTime }}<em>秒</em></span>
              </div>
              <div class="stat-card">
                <span class="stat-label">今日吊次</span>
                <span class="stat-value">{{ workInfo.totalTimes }}<em>次</em></span>
              </div>
              <div class="stat-card">
                <span class="stat-label">今日吊重</span>
                <span class="stat-value">{{ workInfo.totalLoad }}<em>t</em></span>
              </div>
              <div class="stat-card">
                <span class="stat-label">今日功效</span>
                <span class="stat-value">{{ workInfo.totalEffect }}<em>%</em></span>
              </div>
              <div class="stat-card">
                <span class="stat-label">今日报警</span>
                <span class="stat-value warn">{{ workInfo.totalAlarm }}<em>次</em></span>
              </div>
            </div>

            <div class="status-right">
              <div ref="rotateChartRef" class="chart chart-rotate"></div>
              <div class="rotate-value">
                <span class="rotate-num">{{ realtimeData.rotation }}</span>
                <span class="rotate-unit">° 回转角</span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时工作数据
          </div>
          <div class="realtime-grid">
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/xzimg.png" alt="吊重" /></div>
              <div><span class="rt-value">{{ realtimeData.weight }}</span><span class="rt-unit">t</span></div>
              <div class="rt-label">吊重</div>
            </div>
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/tbgico.png" alt="幅度" /></div>
              <div><span class="rt-value">{{ realtimeData.amplitude }}</span><span class="rt-unit">m</span></div>
              <div class="rt-label">幅度</div>
            </div>
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/shebicon1.png" alt="倾度" /></div>
              <div><span class="rt-value">{{ realtimeData.obliguity }}</span><span class="rt-unit">°</span></div>
              <div class="rt-label">倾度</div>
            </div>
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/fengsu.png" alt="风速" /></div>
              <div><span class="rt-value">{{ realtimeData.windSpeed }}</span><span class="rt-unit">m/s</span></div>
              <div class="rt-label">风速</div>
            </div>
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/qbcico.png" alt="力矩" /></div>
              <div><span class="rt-value">{{ realtimeData.moment }}</span><span class="rt-unit">M·T</span></div>
              <div class="rt-label">力矩</div>
            </div>
            <div class="rt-card">
              <div class="rt-icon"><img src="../assets/img/dganb.png" alt="高度" /></div>
              <div><span class="rt-value">{{ realtimeData.height }}</span><span class="rt-unit">m</span></div>
              <div class="rt-label">高度</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：实时力矩趋势 / 实时吊重趋势 -->
      <div class="tow-col tow-col-right">
        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时力矩趋势
          </div>
          <div ref="momentChartRef" class="chart chart-side"></div>
        </div>

        <div class="panel tow-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时吊重趋势
          </div>
          <div ref="weightChartRef" class="chart chart-side"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { selectAllTower, getTowerData, getTowerWorkInfo, getTowerAlarmInfo } from '@/api/tower'

const props = defineProps({
  initialAssetId: { type: Number, default: null }
})

// ---------------- 数据 ----------
const towerListData = ref([])
const activeTab = ref(0)
const onlineMap = ref({})

const towerData = reactive({
  name: '', assetCode: '', maxWeight: '', ratedMoment: '',
  foreArmLength: '', rearArmLength: '', maxHeight: '', ratedLoad: ''
})
const workInfo = reactive({ totalWorkTime: 0, totalTimes: 0, totalAlarm: 0, totalLoad: 0, totalEffect: 0 })
const alarmInfo = reactive({ todayWarning: 0, totalWarning: 0, todayAlarm: 0, totalAlarm: 0 })
const realtimeData = reactive({ monitorTime: '', weight: '', amplitude: '', obliguity: '', windSpeed: '', moment: '', height: '', rotation: 0 })

const alarmRange = ref(30)

// ---------------- 图表 ----------
const alarmChartRef = ref(null)
const momentChartRef = ref(null)
const weightChartRef = ref(null)
const rotateChartRef = ref(null)

let alarmChart = null
let momentChart = null
let weightChart = null
let rotateChart = null
let timer = null
let resizeHandler = null

const chartColors = {
  primary: '#b07b3e',
  orange: '#f5a623',
  red: '#e54545',
  green: '#0d9e6c',
  blue: '#5a8fd4',
  gridLine: 'rgba(122,106,85,0.18)',
  textDim: '#9a8f7f'
}

// ---------------- 工具 ----------
const fmtTime = (t) => (t ? String(t).slice(11, 16) : '')
const fmtDay = (d) => (d ? String(d).slice(5, 10) : '')
const fmtDateTime = (t) => (t ? String(t).replace('T', ' ') : '')

function baseLineOption(name, color, data) {
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    grid: { top: 24, right: 18, bottom: 28, left: 44 },
    xAxis: {
      type: 'category',
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
      name,
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '26' },
          { offset: 1, color: color + '00' }
        ])
      }
    }]
  }
}

// ---------------- 数据加载 ----------
async function loadTowerData(assetId) {
  if (!assetId) return
  const [dataRes, workRes, alarmRes] = await Promise.all([
    getTowerData(assetId),
    getTowerWorkInfo(assetId),
    getTowerAlarmInfo(assetId)
  ])

  const realtimeList = (dataRes && dataRes.data) || []
  // 按时间升序，最新一条在尾部
  const sorted = [...realtimeList].sort((a, b) => new Date(a.monitorTime) - new Date(b.monitorTime))
  const latest = sorted[sorted.length - 1] || {}

  onlineMap.value = { ...onlineMap.value, [assetId]: sorted.length > 0 }
  Object.assign(realtimeData, {
    monitorTime: latest.monitorTime ? fmtDateTime(latest.monitorTime) : '',
    weight: latest.weight ?? '--',
    amplitude: latest.amplitude ?? '--',
    obliguity: latest.obliguity ?? '--',
    windSpeed: latest.windSpeed ?? '--',
    moment: latest.moment ?? '--',
    height: latest.height ?? '--',
    rotation: latest.rotation ?? 0
  })

  const work = (workRes && workRes.data) || {}
  Object.assign(workInfo, {
    totalWorkTime: work.totalWorkTime ?? 0,
    totalTimes: work.totalTimes ?? 0,
    totalAlarm: work.totalAlarm ?? 0,
    totalLoad: work.totalLoad ?? 0,
    totalEffect: work.totalEffect ?? 0
  })

  const alarm = (alarmRes && alarmRes.data) || {}
  Object.assign(alarmInfo, {
    todayWarning: alarm.todayWarning ?? 0,
    totalWarning: alarm.totalWarning ?? 0,
    todayAlarm: alarm.todayAlarm ?? 0,
    totalAlarm: alarm.totalAlarm ?? 0
  })
  renderAlarmChart(alarm)

  renderTrendCharts(sorted)
}

// 报警趋势：30日全量，7日截取末尾7天
function renderAlarmChart(alarm) {
  if (!alarmChart) return
  let days = Array.isArray(alarm.days30Alarm) ? [...alarm.days30Alarm] : []
  const warnMap = {}
  ;(Array.isArray(alarm.days30Warning) ? alarm.days30Warning : []).forEach((w) => {
    warnMap[w.alarmDate] = w.alarmCount
  })
  if (alarmRange.value === 7) days = days.slice(-7)
  days.sort((a, b) => new Date(a.alarmDate) - new Date(b.alarmDate))

  const cats = days.map((d) => fmtDay(d.alarmDate))
  const warnData = cats.map((_, i) => warnMap[days[i].alarmDate] ?? null)
  const alarmData = days.map((d) => d.alarmCount)

  alarmChart.setOption(baseLineOption('报警', chartColors.red, alarmData), true)
  alarmChart.setOption({
    xAxis: { data: cats }
  })
  alarmChart.setOption({
    series: [
      baseLineOption('报警', chartColors.red, alarmData).series[0],
      baseLineOption('预警', chartColors.primary, warnData).series[0]
    ],
    legend: {
      top: 0, right: 8,
      textStyle: { color: chartColors.textDim, fontSize: 11 },
      itemWidth: 12, itemHeight: 6
    }
  })
}

function renderTrendCharts(sorted) {
  if (!momentChart || !weightChart) return
  const cats = sorted.map((d) => fmtTime(d.monitorTime))
  momentChart.setOption(baseLineOption('力矩', chartColors.orange, sorted.map((d) => d.moment)), true)
  momentChart.setOption({ xAxis: { data: cats } })
  weightChart.setOption(baseLineOption('吊重', chartColors.blue, sorted.map((d) => d.weight)), true)
  weightChart.setOption({ xAxis: { data: cats } })
}

function renderRotate(angle) {
  if (!rotateChart) return
  const val = Number(angle) || 0
  rotateChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      min: 0, max: 360,
      progress: { show: true, width: 10, itemStyle: { color: chartColors.primary } },
      axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(176,123,62,0.18)']] } },
      pointer: { show: true, width: 4, length: '60%', itemStyle: { color: chartColors.primary } },
      axisTick: { distance: -14, splitNumber: 6, lineStyle: { color: chartColors.textDim, width: 1 } },
      splitLine: { distance: -16, length: 5, lineStyle: { color: chartColors.textDim, width: 2 } },
      axisLabel: { distance: 8, color: chartColors.textDim, fontSize: 9 },
      anchor: { show: true, size: 6, itemStyle: { color: '#fff', borderColor: chartColors.primary, borderWidth: 2 } },
      detail: { show: false },
      data: [{ value: val }]
    }]
  })
}

// ---------------- 切换 ----------
function switchRange(days) {
  alarmRange.value = days
  if (alarmInfo) renderAlarmChart(alarmInfo)
}

function switchTower(index) {
  activeTab.value = index
  const item = towerListData.value[index]
  if (!item) return
  applyTowerInfo(item)
  loadTowerData(item.assetId)
}

function applyTowerInfo(item) {
  Object.assign(towerData, {
    name: item.assetName || '',
    assetCode: item.assetId ?? '',
    maxWeight: item.maxLoad ?? '--',
    ratedMoment: item.ratedMoment ?? '--',
    foreArmLength: item.forwardArmLength ?? '--',
    rearArmLength: item.backwardArmLength ?? '--',
    maxHeight: item.maxHeight ?? '--',
    ratedLoad: item.ratedLoad ?? '--'
  })
}

async function init() {
  const res = await selectAllTower()
  const list = (res && res.data) || []
  towerListData.value = list
  if (list.length === 0) return

  // 优先选中通过总览点击传入的塔吊，否则默认第一台
  let idx = 0
  const preset = props.initialAssetId
  if (preset) {
    const found = list.findIndex((t) => t.assetId == preset)
    if (found >= 0) idx = found
  }
  activeTab.value = idx
  applyTowerInfo(list[idx])
  await loadTowerData(list[idx].assetId)
  nextTick(() => {
    renderRotate(realtimeData.rotation)
  })
}

// ---------------- 生命周期 ----------
onMounted(() => {
  nextTick(() => {
    alarmChart = echarts.init(alarmChartRef.value)
    momentChart = echarts.init(momentChartRef.value)
    weightChart = echarts.init(weightChartRef.value)
    rotateChart = echarts.init(rotateChartRef.value)
  }).then(() => init())

  timer = setInterval(() => {
    const item = towerListData.value[activeTab.value]
    if (item) {
      loadTowerData(item.assetId).then(() => {
        renderRotate(realtimeData.rotation)
      })
    }
  }, 10000)

  resizeHandler = () => { [alarmChart, momentChart, weightChart, rotateChart].forEach((c) => c && c.resize()) }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  ;[alarmChart, momentChart, weightChart, rotateChart].forEach((c) => c && c.dispose())
  alarmChart = momentChart = weightChart = rotateChart = null
})
</script>

<style scoped>
.tow-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}
.tow-head {
  padding: 6px 4px 10px;
}
.tow-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-muted);
}
.tow-body {
  flex: 1;
  display: flex;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}
.tow-col {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
.tow-col-left { flex: 1 1 0; }
.tow-col-center { flex: 3 1 0; }
.tow-col-right { flex: 1 1 0; }

.panel {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
}
.tow-panel { padding: 12px; flex-shrink: 0; }
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

/* 左栏：报警统计 */
.alarm-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.alarm-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 6px;
  background: var(--bg-soft);
  border-radius: 8px;
}
.alarm-icon { width: 34px; height: 34px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.alarm-icon img { width: 22px; height: 22px; }
.alarm-warn { background: rgba(245, 166, 35, 0.14); }
.alarm-alarm { background: rgba(229, 69, 69, 0.12); }
.alarm-num { font-size: 20px; font-weight: 700; color: var(--text-strong); margin-top: 6px; }
.alarm-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

/* 报警趋势 */
.range-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}
.range-btn {
  padding: 3px 12px;
  font-size: 12px;
  border-radius: 14px;
  color: var(--text-secondary);
  background: var(--bg-soft);
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.range-btn.active {
  color: #fff;
  background: var(--primary-grad);
}
.chart-trend { height: 200px; width: 100%; }

/* 设备信息 */
.info-table { width: 100%; }
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 7px 2px;
  border-bottom: 1px solid var(--border);
  font-size: 12px;
}
.info-row:last-child { border-bottom: none; }
.info-key { color: var(--text-secondary); }
.info-val { color: var(--text-main); font-weight: 500; }

/* 塔吊切换 */
.tower-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 2px;
}
.tower-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-white);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}
.tower-tab.active {
  color: #fff;
  background: var(--primary-grad);
  border-color: transparent;
}
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.on { background: var(--success); }
.status-dot.off { background: var(--text-faint); }

/* 设备信息与状态 */
.status-grid {
  display: grid;
  grid-template-columns: 150px 1fr 190px;
  gap: 14px;
  align-items: center;
}
.status-left { display: flex; flex-direction: column; align-items: center; }
.device-img-wrap {
  width: 120px; height: 120px;
  background: var(--bg-soft);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.device-img { width: 96px; height: auto; }
.device-no, .device-time { font-size: 11px; color: var(--text-secondary); margin-top: 4px; }

.status-mid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.stat-card {
  background: var(--bg-soft);
  border-radius: 8px;
  padding: 12px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-label { font-size: 11px; color: var(--text-secondary); }
.stat-value { font-size: 16px; font-weight: 700; color: var(--text-strong); margin-top: 6px; }
.stat-value em { font-style: normal; font-size: 10px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
.stat-value.warn { color: var(--danger); }

.status-right { display: flex; flex-direction: column; align-items: center; }
.chart-rotate { width: 170px; height: 130px; }
.rotate-value { text-align: center; }
.rotate-num { font-size: 22px; font-weight: 700; color: var(--primary-text); }
.rotate-unit { font-size: 11px; color: var(--text-secondary); }

/* 实时工作数据 */
.realtime-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}
.rt-card {
  background: var(--bg-soft);
  border-radius: 8px;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.rt-icon { width: 30px; height: 30px; border-radius: 8px; background: var(--bg-white); display: flex; align-items: center; justify-content: center; margin-bottom: 6px; }
.rt-icon img { width: 20px; height: 20px; }
.rt-value { font-size: 18px; font-weight: 700; color: var(--text-strong); }
.rt-unit { font-size: 10px; color: var(--text-secondary); margin-left: 2px; }
.rt-label { font-size: 11px; color: var(--text-secondary); margin-top: 4px; }

/* 右侧趋势 */
.chart-side { height: 250px; width: 100%; }
</style>
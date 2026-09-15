<template>
  <div class="elv-wrap">
    <div class="elv-head">
      <h2 class="elv-title"> 施工设备 > 升降机监测分析 </h2>
    </div>

    <div class="elv-body">
      <!-- 左栏：设备报警 / 报警趋势 / 设备信息 -->
      <div class="elv-col elv-col-left">
        <div class="panel elv-panel">
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

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            报警趋势
          </div>
          <div class="range-switch">
            <span class="range-btn" :class="{ active: alarmRange === 30 }" @click="switchRange(30)">近30日</span>
            <span class="range-btn" :class="{ active: alarmRange === 7 }" @click="switchRange(7)">近7日</span>
          </div>
          <div ref="alarmChartRef" class="chart chart-trend"></div>
        </div>

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            设备信息
          </div>
          <div class="info-table">
            <div class="info-row"><span class="info-key">设备名称</span><span class="info-val">{{ liftInfo.assetName }}</span></div>
            <div class="info-row"><span class="info-key">设备编号</span><span class="info-val">{{ liftInfo.assetCode }}</span></div>
            <div class="info-row"><span class="info-key">额定载重</span><span class="info-val">{{ liftInfo.ratedWeight }} t</span></div>
            <div class="info-row"><span class="info-key">额定速度</span><span class="info-val">{{ liftInfo.ascendingSpeed }} m/s</span></div>
            <div class="info-row"><span class="info-key">吊笼尺寸</span><span class="info-val">{{ liftInfo.cabinSize }}</span></div>
            <div class="info-row"><span class="info-key">额定人数</span><span class="info-val">{{ liftInfo.ratedLoad }} 人</span></div>
            <div class="info-row"><span class="info-key">最大高度</span><span class="info-val">{{ liftInfo.maxAscendingHeight }} m</span></div>
            <div class="info-row"><span class="info-key">基本高度</span><span class="info-val">{{ liftInfo.basicHeight }} m</span></div>
          </div>
        </div>
      </div>

      <!-- 中栏：升降机切换 / 实时数据监控 / 运行状态 -->
      <div class="elv-col elv-col-center">
        <div class="lift-tabs">
          <div
            v-for="(item, index) in liftListData"
            :key="item.assetId"
            class="lift-tab"
            :class="{ active: activeTab === index }"
            @click="switchLift(index)"
          >
            <span class="status-dot" :class="statusClass(statusMap[item.assetId])"></span>
            {{ item.assetName || `升降机${index + 1}` }}
          </div>
        </div>

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时数据监控
          </div>
          <div class="monitor-box">
            <div class="lift-legend">
              <div class="legend-item">
                <div class="legend-icon"><img src="../assets/img/lxico.png" alt="重量" /></div>
                <div class="legend-num">{{ realtimeData.weight }}<em>t</em></div>
                <div class="legend-label">实时重量</div>
              </div>
              <div class="legend-item">
                <div class="legend-icon"><img src="../assets/img/dganb.png" alt="高度" /></div>
                <div class="legend-num">{{ realtimeData.height }}<em>m</em></div>
                <div class="legend-label">实时高度</div>
              </div>
            </div>
            <div class="lift-img-wrap">
              <img src="../assets/img/shengjiangji.png" alt="升降机" class="lift-img" />
            </div>
            <div class="lift-legend">
              <div class="legend-item">
                <div class="legend-icon"><img src="../assets/img/fengsu.png" alt="速度" /></div>
                <div class="legend-num">{{ realtimeData.speed }}<em>m/s</em></div>
                <div class="legend-label">实时速度</div>
              </div>
              <div class="legend-item">
                <div class="legend-icon"><img src="../assets/img/yxico.png" alt="倾斜" /></div>
                <div class="legend-num">{{ realtimeData.tilt }}<em>°</em></div>
                <div class="legend-label">实时倾斜度</div>
              </div>
              <div class="legend-item">
                <div class="legend-icon"><img src="../assets/img/blico.png" alt="人数" /></div>
                <div class="legend-num">{{ realtimeData.numberOfPeople }}<em>人</em></div>
                <div class="legend-label">实时人数</div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            运行状态
          </div>
          <div class="run-grid">
            <div class="run-item">
              <span class="run-label">数据时间</span>
              <span class="run-value">{{ realtimeData.monitorTime || '--' }}</span>
            </div>
            <div class="run-item">
              <span class="run-label">前门锁</span>
              <span class="run-value" :class="lockClass(realtimeData.beforeLock)">{{ lockText(realtimeData.beforeLock) }}</span>
            </div>
            <div class="run-item">
              <span class="run-label">后门锁</span>
              <span class="run-value" :class="lockClass(realtimeData.afterLock)">{{ lockText(realtimeData.afterLock) }}</span>
            </div>
            <div class="run-item">
              <span class="run-label">起升方向</span>
              <span class="run-value">{{ directionText(realtimeData.liftingDirection) }}</span>
            </div>
            <div class="run-item">
              <span class="run-label">载重百分比</span>
              <span class="run-value">{{ realtimeData.weightPercent }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：实时速度 / 实时载重 / 实时高度 -->
      <div class="elv-col elv-col-right">
        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时速度
          </div>
          <div ref="speedChartRef" class="chart chart-side"></div>
        </div>

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时载重
          </div>
          <div ref="weightChartRef" class="chart chart-side"></div>
        </div>

        <div class="panel elv-panel">
          <div class="panel-title">
            <img src="../assets/img/Symbol.png" alt="图标" class="panel-title-icon" />
            实时高度
          </div>
          <div ref="heightChartRef" class="chart chart-side"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { selectAllElevator, getElevatorData, getElevatorAlarmInfo } from '@/api/elevator'

const props = defineProps({
  initialAssetId: { type: Number, default: null }
})

// ---------------- 数据 ----------
const liftListData = ref([])
const activeTab = ref(0)
const statusMap = ref({})

const liftInfo = reactive({
  assetName: '', assetCode: '', ratedWeight: '', basicHeight: '',
  ascendingSpeed: '', ratedLoad: '', cabinSize: '', maxAscendingHeight: ''
})
const alarmInfo = reactive({ todayWarning: 0, totalWarning: 0, todayAlarm: 0, totalAlarm: 0 })
const realtimeData = reactive({
  monitorTime: '', weight: '', height: '', speed: '', tilt: 0,
  numberOfPeople: '', beforeLock: '', afterLock: '', liftingDirection: '', weightPercent: ''
})

const alarmRange = ref(30)

// ---------------- 图表 ----------
const alarmChartRef = ref(null)
const speedChartRef = ref(null)
const weightChartRef = ref(null)
const heightChartRef = ref(null)

let alarmChart = null
let speedChart = null
let weightChart = null
let heightChart = null
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

// 状态圆点：0 正常绿 / 1 黄 / 2 红；无数据灰
function statusClass(v) {
  if (v == null) return 'off'
  if (v === '1') return 'warn'
  if (v === '2') return 'danger'
  return 'on'
}
const lockText = (v) => (v === '1' ? '开启' : v === '0' ? '关闭' : '--')
const lockClass = (v) => (v === '1' ? 'danger-text' : '')
const directionText = (v) => (v === '2' ? '上行 ↑' : v === '3' ? '下行 ↓' : '静止')

function baseAreaOption(name, color, data) {
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
      symbol: 'none',
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '30' },
          { offset: 1, color: color + '00' }
        ])
      }
    }]
  }
}

// ---------------- 数据加载 ----------
async function loadLiftData(assetId) {
  if (!assetId) return
  const [dataRes, alarmRes] = await Promise.all([
    getElevatorData(assetId),
    getElevatorAlarmInfo(assetId)
  ])

  const realtimeList = (dataRes && dataRes.data) || []
  const sorted = [...realtimeList].sort((a, b) => new Date(a.monitorTime) - new Date(b.monitorTime))
  const latest = sorted[sorted.length - 1] || {}

  const warn = latest.warningState
  statusMap.value = { ...statusMap.value, [assetId]: warn ?? null }

  Object.assign(realtimeData, {
    monitorTime: latest.monitorTime ? String(latest.monitorTime).replace('T', ' ') : '',
    weight: latest.weight ?? '--',
    height: latest.height ?? '--',
    speed: latest.speed ?? '--',
    tilt: latest.xAngle ?? latest.yAngle ?? 0,
    numberOfPeople: latest.numberOfPeople ?? '--',
    beforeLock: latest.beforeLock ?? '',
    afterLock: latest.afterLock ?? '',
    liftingDirection: latest.liftingDirection ?? '',
    weightPercent: latest.weightPercent ?? '--'
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

// 报警趋势：双线（预警 + 报警），30日全量，7日截取末尾
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

  alarmChart.setOption(baseAreaOption('报警', chartColors.red, alarmData), true)
  alarmChart.setOption({ xAxis: { data: cats }, series: [
    baseAreaOption('报警', chartColors.red, alarmData).series[0],
    baseAreaOption('预警', chartColors.primary, warnData).series[0]
  ], legend: {
    top: 0, right: 8,
    textStyle: { color: chartColors.textDim, fontSize: 11 },
    itemWidth: 12, itemHeight: 6
  } })
}

function renderTrendCharts(sorted) {
  if (!speedChart || !weightChart || !heightChart) return
  const cats = sorted.map((d) => fmtTime(d.monitorTime))
  speedChart.setOption(baseAreaOption('速度', chartColors.blue, sorted.map((d) => d.speed)), true)
  speedChart.setOption({ xAxis: { data: cats } })
  weightChart.setOption(baseAreaOption('载重', chartColors.green, sorted.map((d) => d.weight)), true)
  weightChart.setOption({ xAxis: { data: cats } })
  heightChart.setOption(baseAreaOption('高度', chartColors.orange, sorted.map((d) => d.height)), true)
  heightChart.setOption({ xAxis: { data: cats } })
}

// ---------------- 切换 ----------
function switchRange(days) {
  alarmRange.value = days
  if (alarmInfo) renderAlarmChart(alarmInfo)
}

function switchLift(index) {
  activeTab.value = index
  const item = liftListData.value[index]
  if (!item) return
  applyLiftInfo(item)
  loadLiftData(item.assetId)
}

function applyLiftInfo(item) {
  Object.assign(liftInfo, {
    assetName: item.assetName || '',
    assetCode: item.assetCode || '',
    ratedWeight: item.ratedWeight ?? '--',
    basicHeight: item.basicHeight ?? '--',
    ascendingSpeed: item.ascendingSpeed ?? '--',
    ratedLoad: item.ratedLoad ?? '--',
    cabinSize: item.cabinSize ?? '--',
    maxAscendingHeight: item.maxAscendingHeight ?? '--'
  })
}

async function init() {
  const res = await selectAllElevator()
  const list = (res && res.data) || []
  liftListData.value = list
  if (list.length === 0) return

  let idx = 0
  const preset = props.initialAssetId
  if (preset) {
    const found = list.findIndex((t) => t.assetId == preset)
    if (found >= 0) idx = found
  }
  activeTab.value = idx
  applyLiftInfo(list[idx])
  await loadLiftData(list[idx].assetId)
}

// ---------------- 生命周期 ----------
onMounted(() => {
  nextTick(() => {
    alarmChart = echarts.init(alarmChartRef.value)
    speedChart = echarts.init(speedChartRef.value)
    weightChart = echarts.init(weightChartRef.value)
    heightChart = echarts.init(heightChartRef.value)
  }).then(() => init())

  timer = setInterval(() => {
    const item = liftListData.value[activeTab.value]
    if (item) loadLiftData(item.assetId)
  }, 10000)

  resizeHandler = () => { [alarmChart, speedChart, weightChart, heightChart].forEach((c) => c && c.resize()) }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  ;[alarmChart, speedChart, weightChart, heightChart].forEach((c) => c && c.dispose())
  alarmChart = speedChart = weightChart = heightChart = null
})
</script>

<style scoped>
.elv-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}
.elv-head { padding: 6px 4px 10px; }
.elv-title { font-size: 15px; font-weight: 600; color: var(--text-muted); }
.elv-body {
  flex: 1;
  display: flex;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}
.elv-col {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
.elv-col-left { flex: 1 1 0; }
.elv-col-center { flex: 3 1 0; }
.elv-col-right { flex: 1 1 0; }

.panel {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
}
.elv-panel { padding: 12px; flex-shrink: 0; }
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
.range-switch { display: flex; gap: 8px; margin-bottom: 4px; }
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
.range-btn.active { color: #fff; background: var(--primary-grad); }
.chart-trend { height: 180px; width: 100%; }

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

/* 切换 */
.lift-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 2px;
}
.lift-tab {
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
.lift-tab.active { color: #fff; background: var(--primary-grad); border-color: transparent; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.on { background: var(--success); }
.status-dot.warn { background: #f5a623; }
.status-dot.danger { background: var(--danger); }
.status-dot.off { background: var(--text-faint); }

/* 实时数据监控 */
.monitor-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.lift-legend { display: flex; flex-direction: column; gap: 12px; }
.legend-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 92px;
  padding: 8px 4px;
  background: var(--bg-soft);
  border-radius: 8px;
}
.legend-icon { width: 26px; height: 26px; border-radius: 6px; background: var(--bg-white); display: flex; align-items: center; justify-content: center; margin-bottom: 4px; }
.legend-icon img { width: 18px; height: 18px; }
.legend-num { font-size: 16px; font-weight: 700; color: var(--text-strong); }
.legend-num em { font-style: normal; font-size: 10px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
.legend-label { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.lift-img-wrap {
  width: 170px;
  height: 200px;
  background: var(--bg-soft);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lift-img { width: 130px; height: auto; }

/* 运行状态 */
.run-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
.run-item {
  background: var(--bg-soft);
  border-radius: 8px;
  padding: 12px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.run-label { font-size: 11px; color: var(--text-secondary); }
.run-value { font-size: 14px; font-weight: 600; color: var(--text-strong); margin-top: 6px; }
.run-value.danger-text { color: var(--danger); }

/* 右侧趋势 */
.chart-side { height: 150px; width: 100%; }
</style>
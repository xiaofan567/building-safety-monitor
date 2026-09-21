<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/login'
import MainPage from '@/views/MainPage.vue'
import EquipmentType from '@/views/EquipmentType.vue'
import DeviceSimulation from '@/views/DeviceSimulation.vue'
import EquipmentLocation from '@/views/EquipmentLocation.vue'
import EquipmentAssets from '@/views/EquipmentAssets.vue'
import EquipmentOverview from '@/views/EquipmentOverview.vue'
import TowerMonitoring from '@/views/TowerMonitoring.vue'
import ElevatorMonitoring from '@/views/ElevatorMonitoring.vue'
import FormworkMonitoring from '@/views/FormworkMonitoring.vue'
import FoundationPitMonitoring from '@/views/FoundationPitMonitoring.vue'
import EquipmentOnOfflineRecord from '@/views/EquipmentOnOfflineRecord.vue'
import MonitoringAlarmRecord from '@/views/MonitoringAlarmRecord.vue'
import DeviceMonitoringPointMng from '@/views/DeviceMonitoringPointMng.vue'
import EnvironmentMonitoringMng from '@/views/EnvironmentMonitoringMng.vue'
import EnvironmentalMonitoring from '@/views/EnvironmentalMonitoring.vue'
import EnvironmentalHistoryData from '@/views/EnvironmentalHistoryData.vue'
import SprayTask from '@/views/SprayTask.vue'
import EnvironmentalSprayingRecord from '@/views/EnvironmentalSprayingRecord.vue'
import EnvironmentAlarmRecord from '@/views/EnvironmentAlarmRecord.vue'
import VideoSurveillance from '@/views/VideoSurveillance.vue'
import RiskRecognitionSettings from '@/views/RiskRecognitionSettings.vue'
import AIRiskRecognition from '@/views/AIRiskRecognition.vue'

const router = useRouter()
const route = useRoute()
const store = useStore()
const currentComponent = ref('MainPage')
const activeMenu = ref('dashboard')
const overviewType = ref(5)
const towerAssetId = ref(null)
const liftAssetId = ref(null)
const formworkAssetId = ref(null)
const pitAssetId = ref(null)

const constructionMenuVisible = ref(false)
const equipmentMenuVisible = ref(false)
const userMenuVisible = ref(false)
const deviceMenuVisible = ref(false)
const monitorConfigVisible = ref(false)
const constructionEnvVisible = ref(false)

const siteOptions = [
  { label: '现场监控', value: 'VideoSurveillance' },
  { label: '风险识别设置', value: 'RiskRecognitionSettings' },
  { label: 'AI风险识别与处理', value: 'AIRiskRecognition' }
]

const equipmentOptions = [
  { label: '机械设备总览', value: 5 },
  { label: '塔吊', value: 1 },
  { label: '升降机', value: 2 },
  { label: '高支模', value: 4 },
  { label: '深基坑', value: 3 },
  { label: '设备上下线记录', value: 6 },
  { label: '监测报警记录', value: 7 }
]

const menuItems = [
  { key: 'dashboard', label: '监控总览', icon: 'overview', comp: 'MainPage' },
  { key: 'simulation', label: '设备模拟', icon: 'simulation', comp: 'DeviceSimulation' }
]

// 页面组件名 -> 顶部菜单高亮 key 的映射
const pageMenuMap = {
  MainPage: 'dashboard',
  DeviceSimulation: 'simulation',
  EquipmentOverview: 'equipment',
  TowerMonitoring: 'equipment',
  ElevatorMonitoring: 'equipment',
  FormworkMonitoring: 'equipment',
  FoundationPitMonitoring: 'equipment',
  EquipmentOnOfflineRecord: 'equipment',
  MonitoringAlarmRecord: 'equipment',
  EquipmentType: 'device',
  EquipmentLocation: 'device',
  EquipmentAssets: 'device',
  DeviceMonitoringPointMng: 'monitorConfig',
  EnvironmentMonitoringMng: 'monitorConfig',
  EnvironmentalMonitoring: 'constructionEnv',
  EnvironmentalHistoryData: 'constructionEnv',
  SprayTask: 'constructionEnv',
  EnvironmentalSprayingRecord: 'constructionEnv',
  EnvironmentAlarmRecord: 'constructionEnv',
  VideoSurveillance: 'site',
  RiskRecognitionSettings: 'site',
  AIRiskRecognition: 'site'
}

// 统一跳转：把子页面与参数写入 URL query，刷新/直达/前进后退可恢复
function navigateTo(page, extra = {}) {
  router.push({ path: '/home', query: { ...route.query, page, ...extra } })
}

// 从 URL query 恢复子页面与参数
function restoreStateFromQuery() {
  const q = route.query
  currentComponent.value = q.page && q.page !== 'MainPage' ? q.page : 'MainPage'
  activeMenu.value = pageMenuMap[currentComponent.value] || 'dashboard'
  if (q.overviewType) overviewType.value = Number(q.overviewType)
  if (q.assetId) {
    const assetId = Number(q.assetId)
    if (currentComponent.value === 'TowerMonitoring') towerAssetId.value = assetId
    else if (currentComponent.value === 'ElevatorMonitoring') liftAssetId.value = assetId
    else if (currentComponent.value === 'FormworkMonitoring') formworkAssetId.value = assetId
    else if (currentComponent.value === 'FoundationPitMonitoring') pitAssetId.value = assetId
  }
}

onMounted(restoreStateFromQuery)
watch(() => route.query, restoreStateFromQuery)

const getCurrentComponent = computed(() => {
  const map = {
    MainPage,
    EquipmentType,
    DeviceSimulation,
    EquipmentLocation,
    EquipmentAssets,
    EquipmentOverview,
    TowerMonitoring,
    ElevatorMonitoring,
    FormworkMonitoring,
    FoundationPitMonitoring,
    EquipmentOnOfflineRecord,
    MonitoringAlarmRecord,
    DeviceMonitoringPointMng,
    EnvironmentMonitoringMng,
    EnvironmentalMonitoring,
    EnvironmentalHistoryData,
    SprayTask,
    EnvironmentalSprayingRecord,
    EnvironmentAlarmRecord,
    VideoSurveillance,
    RiskRecognitionSettings,
    AIRiskRecognition
  }
  return map[currentComponent.value] || MainPage
})

// 设备总览组件需要把顶部下拉选中的类型作为初始类型传入
const overviewProps = computed(() => {
  if (currentComponent.value === 'EquipmentOverview') return { initialType: overviewType.value }
  if (currentComponent.value === 'TowerMonitoring') return { initialAssetId: towerAssetId.value }
  if (currentComponent.value === 'ElevatorMonitoring') return { initialAssetId: liftAssetId.value }
  if (currentComponent.value === 'FormworkMonitoring') return { initialAssetId: formworkAssetId.value }
  if (currentComponent.value === 'FoundationPitMonitoring') return { initialAssetId: pitAssetId.value }
  return {}
})

// 从总览点击塔吊图标进入塔机监测分析
function openTowerMonitor(payload) {
  towerAssetId.value = payload ? Number(payload.assetId) : null
  activeMenu.value = 'equipment'
  navigateTo('TowerMonitoring', payload ? { assetId: Number(payload.assetId) } : {})
}

function handleMenuClick(key) {
  const item = menuItems.find(m => m.key === key)
  if (item) {
    navigateTo(item.comp)
  }
}

// 施工场地下拉导航（现场监控 / 风险识别设置 / AI风险识别与处理）
function selectConstructionSite(value) {
  constructionMenuVisible.value = false
  navigateTo(value)
}

async function handleLogout() {
  try {
    await logout()
  } catch (e) {
    // ignore
  }
  store.commit('setToken', '')
  store.commit('setUserInfo', '')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已退出登录')
  router.push('/login')
}

function closeOtherDropdowns() {
  constructionMenuVisible.value = false
  equipmentMenuVisible.value = false
  userMenuVisible.value = false
  deviceMenuVisible.value = false
  monitorConfigVisible.value = false
  constructionEnvVisible.value = false
}

function toggleConstructionMenu() {
  if (constructionMenuVisible.value) {
    constructionMenuVisible.value = false
    return
  }
  closeOtherDropdowns()
  constructionMenuVisible.value = true
}

function toggleEquipmentMenu() {
  if (equipmentMenuVisible.value) {
    equipmentMenuVisible.value = false
    return
  }
  closeOtherDropdowns()
  equipmentMenuVisible.value = true
}

function toggleUserMenu() {
  if (userMenuVisible.value) {
    userMenuVisible.value = false
    return
  }
  closeOtherDropdowns()
  userMenuVisible.value = true
}

function toggleDeviceMenu() {
  if (deviceMenuVisible.value) {
    deviceMenuVisible.value = false
    return
  }
  closeOtherDropdowns()
  deviceMenuVisible.value = true
}

function toggleMonitorConfigMenu() {
  if (monitorConfigVisible.value) {
    monitorConfigVisible.value = false
    return
  }
  closeOtherDropdowns()
  monitorConfigVisible.value = true
}

function toggleConstructionEnvMenu() {
  if (constructionEnvVisible.value) {
    constructionEnvVisible.value = false
    return
  }
  closeOtherDropdowns()
  constructionEnvVisible.value = true
}

// 监测配置下拉 - 选择“设备监测点管理”
function selectMonitorPointMng() {
  monitorConfigVisible.value = false
  navigateTo('DeviceMonitoringPointMng')
}

// 监测配置下拉 - 选择“环境监测管理”
function selectEnvironmentMng() {
  monitorConfigVisible.value = false
  navigateTo('EnvironmentMonitoringMng')
}

// 施工环境下拉 - 选择“环境实时监测”
function selectEnvironmentalMonitoring() {
  constructionEnvVisible.value = false
  navigateTo('EnvironmentalMonitoring')
}

// 施工环境下拉 - 选择“环境监测历史数据”
function selectEnvironmentalHistoryData() {
  constructionEnvVisible.value = false
  navigateTo('EnvironmentalHistoryData')
}

// 施工环境下拉 - 选择“喷淋任务”
function selectSprayTask() {
  constructionEnvVisible.value = false
  navigateTo('SprayTask')
}

// 施工环境下拉 - 选择“自动喷淋记录”
function selectAutoSprayRecord() {
  constructionEnvVisible.value = false
  navigateTo('EnvironmentalSprayingRecord')
}

// 施工环境下拉 - 选择“环境监测报警记录”
function selectEnvironmentAlarmRecord() {
  constructionEnvVisible.value = false
  navigateTo('EnvironmentAlarmRecord')
}

// 设备管理下拉 - 选择“设备类型管理”
function selectDeviceType() {
  deviceMenuVisible.value = false
  navigateTo('EquipmentType')
}

// 设备管理下拉 - 选择“设备位置管理”
function selectEquipmentLocation() {
  deviceMenuVisible.value = false
  navigateTo('EquipmentLocation')
}

// 设备管理下拉 - 选择“设备管理”
function selectEquipmentAssets() {
  deviceMenuVisible.value = false
  navigateTo('EquipmentAssets')
}

// 施工设备下拉 - 选择设备查看入口
function selectOverview(type) {
  equipmentMenuVisible.value = false
  activeMenu.value = 'equipment'
  if (type === 5) {
    // 机械设备总览：保持总览页不变
    overviewType.value = 5
    navigateTo('EquipmentOverview', { overviewType: 5 })
  } else if (type === 1) {
    // 塔吊：进入塔机监测分析页（任务6-2）
    towerAssetId.value = null
    navigateTo('TowerMonitoring')
  } else if (type === 2) {
    // 升降机：进入升降机监测分析页（任务6-3）
    liftAssetId.value = null
    navigateTo('ElevatorMonitoring')
  } else if (type === 4) {
    // 高支模：进入高支模监测分析页（任务6-4）
    formworkAssetId.value = null
    navigateTo('FormworkMonitoring')
  } else if (type === 3) {
    // 深基坑：进入深基坑监测分析页（任务6-5）
    pitAssetId.value = null
    navigateTo('FoundationPitMonitoring')
  } else if (type === 6) {
    // 设备上下线记录：进入设备上下线记录页（任务6-6）
    navigateTo('EquipmentOnOfflineRecord')
  } else if (type === 7) {
    // 监测报警记录：进入监测报警记录页（任务6-7）
    navigateTo('MonitoringAlarmRecord')
  }
}

function selectDeviceMonitoringPoint() {
  deviceMenuVisible.value = false
  activeMenu.value = 'device'
  currentComponent.value = 'DeviceMonitoringPointMng'
}

function handleClickOutside(event) {
  const constructionEl = document.querySelector('.nav-dropdown-construction')
  const equipmentEl = document.querySelector('.nav-dropdown-equipment')
  const userEl = document.querySelector('.nav-dropdown-user')
  const deviceEl = document.querySelector('.nav-dropdown-device')
  const monitorEl = document.querySelector('.nav-dropdown-monitor')
  const constructionEnvEl = document.querySelector('.nav-dropdown-construction-env')
  if (constructionEl && !constructionEl.contains(event.target)) {
    constructionMenuVisible.value = false
  }
  if (equipmentEl && !equipmentEl.contains(event.target)) {
    equipmentMenuVisible.value = false
  }
  if (userEl && !userEl.contains(event.target)) {
    userMenuVisible.value = false
  }
  if (deviceEl && !deviceEl.contains(event.target)) {
    deviceMenuVisible.value = false
  }
  if (monitorEl && !monitorEl.contains(event.target)) {
    monitorConfigVisible.value = false
  }
  if (constructionEnvEl && !constructionEnvEl.contains(event.target)) {
    constructionEnvVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="home-layout">
    <header class="top-navbar">
      <div class="navbar-left">
        <div class="logo-area">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none" width="28" height="28">
            <defs>
              <linearGradient id="navLogoGrad" x1="4" y1="2" x2="20" y2="22" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#c9a26b" />
                <stop offset="100%" stop-color="#b07b3e" />
              </linearGradient>
            </defs>
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" fill="url(#navLogoGrad)" opacity="0.15"/>
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" stroke="url(#navLogoGrad)" stroke-width="1.6" stroke-linejoin="round"/>
            <path d="M9 11.5L11.2 13.7L15.5 9" stroke="url(#navLogoGrad)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="logo-text">建筑安全智能监控平台</span>
        </div>

        <nav class="main-nav">
          <template v-for="item in menuItems" :key="item.key">
            <div
              class="nav-item"
              :class="{ active: activeMenu === item.key }"
              @click="handleMenuClick(item.key)"
            >
              {{ item.label }}
            </div>
          </template>

          <div class="nav-dropdown nav-dropdown-monitor">
            <div
              class="nav-item nav-item-dropdown"
              :class="{ active: activeMenu === 'monitorConfig' }"
              @click.stop="toggleMonitorConfigMenu"
            >
              监测配置
              <svg class="arr" :class="{ rotated: monitorConfigVisible }" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            </div>
            <transition name="dropdown-fade">
              <div class="dropdown-menu" v-show="monitorConfigVisible">
                <div class="dropdown-item" @click.stop="selectMonitorPointMng">设备监测点管理</div>
                <div class="dropdown-item" @click.stop="selectEnvironmentMng">环境监测管理</div>
              </div>
            </transition>
          </div>

          <div class="nav-dropdown nav-dropdown-construction-env">
            <div
              class="nav-item nav-item-dropdown"
              :class="{ active: activeMenu === 'constructionEnv' }"
              @click.stop="toggleConstructionEnvMenu"
            >
              施工环境
              <svg class="arr" :class="{ rotated: constructionEnvVisible }" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            </div>
            <transition name="dropdown-fade">
              <div class="dropdown-menu" v-show="constructionEnvVisible">
                <div class="dropdown-item" @click.stop="selectEnvironmentalMonitoring">环境实时监测</div>
                <div class="dropdown-item" @click.stop="selectEnvironmentalHistoryData">环境监测历史数据</div>
                <div class="dropdown-item" @click.stop="selectSprayTask">喷淋任务</div>
                <div class="dropdown-item" @click.stop="selectAutoSprayRecord">自动喷淋记录</div>
                <div class="dropdown-item" @click.stop="selectEnvironmentAlarmRecord">环境监测报警记录</div>
              </div>
            </transition>
          </div>

          <div class="nav-dropdown nav-dropdown-device">
            <div
              class="nav-item nav-item-dropdown"
              :class="{ active: activeMenu === 'device' }"
              @click.stop="toggleDeviceMenu"
            >
              设备管理
              <svg class="arr" :class="{ rotated: deviceMenuVisible }" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            </div>
            <transition name="dropdown-fade">
              <div class="dropdown-menu" v-show="deviceMenuVisible">
                <div class="dropdown-item" @click.stop="selectDeviceType">设备类型管理</div>
                <div class="dropdown-item" @click.stop="selectEquipmentLocation">设备位置管理</div>
                <div class="dropdown-item" @click.stop="selectEquipmentAssets">设备管理</div>
              </div>
            </transition>
          </div>
        </nav>
      </div>

      <div class="navbar-right">
        <div class="nav-dropdown nav-dropdown-construction">
          <button class="nav-btn" @click.stop="toggleConstructionMenu">
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16"><rect x="2" y="2" width="20" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/><line x1="2" y1="8" x2="22" y2="8" stroke="currentColor" stroke-width="1.5"/><line x1="8" y1="2" x2="8" y2="22" stroke="currentColor" stroke-width="1.5"/></svg>
            施工场地
            <svg class="arr" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </button>
          <transition name="dropdown-fade">
            <div class="dropdown-menu" v-show="constructionMenuVisible">
              <div class="dropdown-item" v-for="opt in siteOptions" :key="opt.value" @click.stop="selectConstructionSite(opt.value)">
                {{ opt.label }}
              </div>
            </div>
          </transition>
        </div>

        <div class="nav-dropdown nav-dropdown-equipment">
          <button class="nav-btn" @click.stop="toggleEquipmentMenu">
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16"><rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M9 9h6M9 13h4M9 17h2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
            施工设备
            <svg class="arr" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </button>
          <transition name="dropdown-fade">
            <div class="dropdown-menu" v-show="equipmentMenuVisible">
              <div class="dropdown-item" :class="{ 'dropdown-item-disabled': opt.disabled }" v-for="opt in equipmentOptions" :key="opt.value" @click.stop="!opt.disabled && selectOverview(opt.value)">
                {{ opt.label }}
                <span v-if="opt.disabled" class="dropdown-tag">待开发</span>
              </div>
            </div>
          </transition>
        </div>

        <div class="nav-dropdown nav-dropdown-user">
          <button class="nav-btn user-btn" @click.stop="toggleUserMenu">
            <div class="avatar">
              <span>管</span>
            </div>
            <span class="user-name">管理员</span>
            <svg class="arr" viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </button>
          <transition name="dropdown-fade">
            <div class="dropdown-menu dropdown-menu-right" v-show="userMenuVisible">
              <div class="dropdown-item" @click.stop="userMenuVisible = false">个人信息</div>
              <div class="dropdown-item" @click.stop="userMenuVisible = false">修改密码</div>
              <div class="dropdown-divider"></div>
              <div class="dropdown-item dropdown-item-danger" @click.stop="handleLogout">退出登录</div>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <main class="main-content">
      <component :is="getCurrentComponent" v-bind="overviewProps" @open-tower="openTowerMonitor" />
    </main>
  </div>
</template>


<style scoped>
.home-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.top-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  z-index: 100;
  position: relative;
  box-shadow: 0 2px 12px rgba(120, 95, 60, 0.05);
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-strong);
  white-space: nowrap;
  letter-spacing: 1px;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-item {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--text-muted);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-item:hover {
  color: var(--text-secondary);
  background: var(--bg-hover);
}

.nav-item.active {
  color: #fff;
  background: var(--primary-grad);
  box-shadow: 0 3px 10px rgba(176, 123, 62, 0.25);
}

.nav-item-dropdown {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.nav-item-dropdown .arr {
  transition: transform 0.2s;
}

.nav-item-dropdown .arr.rotated {
  transform: rotate(180deg);
}

.dropdown-item-disabled,
.dropdown-item-disabled:hover {
  cursor: not-allowed;
  color: var(--text-faint);
  background: transparent;
}

.dropdown-tag {
  margin-left: 8px;
  font-size: 10px;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 8px;
  color: var(--text-muted);
  background: var(--bg-muted);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-dropdown {
  position: relative;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-btn:hover {
  border-color: var(--border-strong);
  color: var(--text-main);
  background: var(--bg-hover);
}

.nav-btn .arr {
  transition: transform 0.2s;
}

.user-btn {
  border: none;
  background: transparent;
  padding: 4px 8px 4px 4px;
  gap: 8px;
}

.user-btn:hover {
  background: var(--bg-hover);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-grad);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 0 0 2px rgba(176, 123, 62, 0.15);
  position: relative;
}

.avatar::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: var(--success);
  border-radius: 50%;
  border: 2px solid #fff;
}

.user-name {
  font-size: 13px;
  color: var(--text-main);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 170px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 6px;
  box-shadow: var(--shadow);
  z-index: 200;
}

.dropdown-menu-right {
  left: auto;
  right: 0;
}

.dropdown-item {
  padding: 9px 14px;
  font-size: 12px;
  color: var(--text-secondary);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.dropdown-item:hover {
  background: var(--bg-hover);
  color: var(--text-main);
}

.dropdown-item-danger {
  color: var(--danger);
}

.dropdown-item-danger:hover {
  background: var(--danger-tint);
  color: var(--danger);
}

.dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: 4px 0;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: all 0.2s;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.main-content {
  flex: 1;
  overflow: hidden;
}
</style>


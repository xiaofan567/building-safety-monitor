<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/login'
import MainPage from '@/views/MainPage.vue'

const router = useRouter()
const currentComponent = ref('MainPage')
const activeMenu = ref('dashboard')

const constructionMenuVisible = ref(false)
const equipmentMenuVisible = ref(false)
const userMenuVisible = ref(false)

const siteOptions = [
  { label: 'A区 - 主体施工区', value: 'area_a' },
  { label: 'B区 - 配套施工区', value: 'area_b' },
  { label: 'C区 - 材料堆放区', value: 'area_c' }
]

const equipmentOptions = [
  { label: '塔吊设备', value: 'tower_crane' },
  { label: '升降机', value: 'elevator' },
  { label: '环境传感器', value: 'sensor' },
  { label: '喷淋设备', value: 'spray' }
]

const menuItems = [
  { key: 'dashboard', label: '监控总览', icon: 'overview', comp: 'MainPage' },
  { key: 'device', label: '设备管理', icon: 'device', comp: 'MainPage' },
  { key: 'environment', label: '环境监控', icon: 'env', comp: 'MainPage' },
  { key: 'personnel', label: '人员安全', icon: 'person', comp: 'MainPage' },
  { key: 'alarm', label: '预警事件', icon: 'alarm', comp: 'MainPage' },
  { key: 'system', label: '系统管理', icon: 'system', comp: 'MainPage' }
]

const getCurrentComponent = computed(() => {
  const map = { 'MainPage': MainPage }
  return map[currentComponent.value] || MainPage
})

function handleMenuClick(key) {
  activeMenu.value = key
  const item = menuItems.find(m => m.key === key)
  if (item) {
    currentComponent.value = item.comp
  }
}

async function handleLogout() {
  try {
    await logout()
  } catch (e) {
    // ignore
  }
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已退出登录')
  router.push('/login')
}

function toggleConstructionMenu() {
  constructionMenuVisible.value = !constructionMenuVisible.value
  equipmentMenuVisible.value = false
  userMenuVisible.value = false
}

function toggleEquipmentMenu() {
  equipmentMenuVisible.value = !equipmentMenuVisible.value
  constructionMenuVisible.value = false
  userMenuVisible.value = false
}

function toggleUserMenu() {
  userMenuVisible.value = !userMenuVisible.value
  constructionMenuVisible.value = false
  equipmentMenuVisible.value = false
}

function handleClickOutside(event) {
  const constructionEl = document.querySelector('.nav-dropdown-construction')
  const equipmentEl = document.querySelector('.nav-dropdown-equipment')
  const userEl = document.querySelector('.nav-dropdown-user')
  if (constructionEl && !constructionEl.contains(event.target)) {
    constructionMenuVisible.value = false
  }
  if (equipmentEl && !equipmentEl.contains(event.target)) {
    equipmentMenuVisible.value = false
  }
  if (userEl && !userEl.contains(event.target)) {
    userMenuVisible.value = false
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
                <stop offset="0%" stop-color="#1a73e8" />
                <stop offset="100%" stop-color="#0d9e6c" />
              </linearGradient>
            </defs>
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" fill="url(#navLogoGrad)" opacity="0.15"/>
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" stroke="url(#navLogoGrad)" stroke-width="1.6" stroke-linejoin="round"/>
            <path d="M9 11.5L11.2 13.7L15.5 9" stroke="url(#navLogoGrad)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="logo-text">建筑安全智能监控平台</span>
        </div>

        <nav class="main-nav">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeMenu === item.key }"
            @click="handleMenuClick(item.key)"
          >
            {{ item.label }}
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
              <div class="dropdown-item" v-for="opt in siteOptions" :key="opt.value" @click.stop="constructionMenuVisible = false">
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
              <div class="dropdown-item" v-for="opt in equipmentOptions" :key="opt.value" @click.stop="equipmentMenuVisible = false">
                {{ opt.label }}
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
      <component :is="getCurrentComponent" />
    </main>
  </div>
</template>

<style scoped>
.home-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #060d17;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.top-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: rgba(10, 22, 37, 0.95);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(26, 115, 232, 0.1);
  flex-shrink: 0;
  z-index: 100;
  position: relative;
}

.top-navbar::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(26, 115, 232, 0.4), rgba(13, 158, 108, 0.2), transparent);
  pointer-events: none;
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

.logo-icon {
  filter: drop-shadow(0 0 6px rgba(26, 115, 232, 0.3));
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #e8edf2;
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
  color: #8899aa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  position: relative;
}

.nav-item:hover {
  color: #bcc8d4;
  background: rgba(26, 115, 232, 0.06);
}

.nav-item.active {
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.1);
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
  background: rgba(10, 22, 37, 0.5);
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 6px;
  color: #bcc8d4;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-btn:hover {
  border-color: rgba(26, 115, 232, 0.4);
  color: #e8edf2;
  background: rgba(26, 115, 232, 0.06);
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
  background: rgba(26, 115, 232, 0.06);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a73e8, #0d9e6c);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
  position: relative;
}

.avatar::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: #0d9e6c;
  border-radius: 50%;
  border: 2px solid #0a1625;
}

.user-name {
  font-size: 13px;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 170px;
  background: #0f1d2e;
  border: 1px solid rgba(26, 115, 232, 0.15);
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(26, 115, 232, 0.05) inset;
  z-index: 200;
}

.dropdown-menu-right {
  left: auto;
  right: 0;
}

.dropdown-item {
  padding: 9px 14px;
  font-size: 12px;
  color: #bcc8d4;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.dropdown-item:hover {
  background: rgba(26, 115, 232, 0.1);
  color: #e8edf2;
}

.dropdown-item-danger {
  color: #e54545;
}

.dropdown-item-danger:hover {
  background: rgba(229, 69, 69, 0.1);
  color: #ff6b6b;
}

.dropdown-divider {
  height: 1px;
  background: rgba(26, 50, 72, 0.5);
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
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { login, getUserInfo } from '@/api/login'

const router = useRouter()
const store = useStore()

const formRef = ref(null)
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    const token = res && res.data && res.data.adminToken
    if (!token) {
      ElMessage.error('未获取到登录凭证')
      return
    }

    store.commit('setToken', token)

    try {
      const info = await getUserInfo()
      if (info && info.data) {
        store.commit('setUserInfo', info.data)
      }
    } catch (e) {
      // 用户信息获取失败不阻断登录流程
    }

    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 登录失败提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-layer">
      <div class="bg-grid"></div>
      <div class="bg-glow glow-1"></div>
      <div class="bg-glow glow-2"></div>
      <div class="bg-glow glow-3"></div>
      <div class="bg-particles">
        <span v-for="n in 20" :key="n" class="particle" :style="{
          left: Math.random() * 100 + '%',
          top: Math.random() * 100 + '%',
          animationDelay: Math.random() * 8 + 's',
          animationDuration: 6 + Math.random() * 6 + 's'
        }"></span>
      </div>
    </div>

    <div class="login-card">
      <div class="card-glow"></div>
      <div class="login-header">
        <div class="logo-badge">
          <div class="logo-ring"></div>
          <svg viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="logoGrad" x1="4" y1="2" x2="20" y2="22" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#c9a26b" />
                <stop offset="100%" stop-color="#b07b3e" />
              </linearGradient>
            </defs>
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" fill="url(#logoGrad)" opacity="0.15" />
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" stroke="url(#logoGrad)" stroke-width="1.6" stroke-linejoin="round" />
            <path d="M9 11.5L11.2 13.7L15.5 9" stroke="url(#logoGrad)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <h1>建筑安全智能监控平台</h1>
        <p>Intelligent Building Safety Monitoring Platform</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            size="large"
            placeholder="请输入账号"
            clearable
          >
            <template #prefix>
              <svg class="field-icon" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5" />
                <path d="M4 20C4 16.5 7.5 14 12 14C16.5 14 20 16.5 20 20" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg class="field-icon" viewBox="0 0 24 24" fill="none">
                <rect x="5" y="10" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.5" />
                <path d="M8 10V7C8 4.8 9.8 3 12 3C14.2 3 16 4.8 16 7V10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="remember">记住账号</el-checkbox>
          <span class="forgot">忘记密码？</span>
        </div>

        <button type="submit" class="login-btn" :class="{ 'is-loading': loading }" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          <span>{{ loading ? '登录中...' : '登 录' }}</span>
        </button>
      </el-form>

      <div class="login-footer">
        <span>建筑安全智能监控平台 © 2026</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(700px 480px at 15% 12%, rgba(201, 162, 107, 0.22), transparent 60%),
    radial-gradient(680px 460px at 88% 85%, rgba(176, 123, 62, 0.16), transparent 60%),
    #f5f2ec;
}

.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(176, 123, 62, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(176, 123, 62, 0.05) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 70% 70% at 50% 50%, black 30%, transparent 70%);
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.glow-1 {
  width: 500px;
  height: 500px;
  top: -180px;
  left: -120px;
  background: radial-gradient(circle, rgba(201, 162, 107, 0.45), transparent 65%);
  animation: floatGlow 12s ease-in-out infinite;
}

.glow-2 {
  width: 400px;
  height: 400px;
  bottom: -140px;
  right: -80px;
  background: radial-gradient(circle, rgba(176, 123, 62, 0.35), transparent 65%);
  animation: floatGlow 14s ease-in-out infinite reverse;
}

.glow-3 {
  width: 300px;
  height: 300px;
  top: 45%;
  right: 15%;
  background: radial-gradient(circle, rgba(214, 178, 132, 0.3), transparent 65%);
  animation: floatGlow 10s ease-in-out infinite 2s;
}

@keyframes floatGlow {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 15px) scale(0.95); }
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  width: 2px;
  height: 2px;
  background: #c9a26b;
  border-radius: 50%;
  opacity: 0;
  animation: particleFloat 8s ease-in-out infinite;
}

@keyframes particleFloat {
  0% { opacity: 0; transform: translateY(0); }
  10% { opacity: 0.5; }
  90% { opacity: 0; }
  100% { opacity: 0; transform: translateY(-60px); }
}

.login-card {
  position: relative;
  z-index: 2;
  width: 420px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 44px 40px 32px;
  box-shadow:
    0 8px 40px rgba(120, 95, 60, 0.20),
    0 0 0 1px rgba(176, 123, 62, 0.06) inset;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 300px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #c9a26b, #b07b3e, transparent);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-badge {
  position: relative;
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-ring {
  position: absolute;
  inset: -4px;
  border-radius: 18px;
  border: 2px solid transparent;
  background: linear-gradient(135deg, rgba(201, 162, 107, 0.35), rgba(176, 123, 62, 0.35)) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}

.logo-badge svg {
  width: 36px;
  height: 36px;
  filter: drop-shadow(0 0 8px rgba(201, 162, 107, 0.4));
}

.login-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-strong);
  letter-spacing: 2px;
}

.login-header p {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  background: var(--bg-soft);
  border-radius: 9px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px var(--border) inset;
  transition: all 0.25s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(176, 123, 62, 0.12);
  background: var(--bg-white);
}

.login-form :deep(.el-input__inner) {
  color: var(--text-main);
}

.login-form :deep(.el-input__inner::placeholder) {
  color: var(--text-faint);
}

.field-icon {
  width: 18px;
  height: 18px;
  color: var(--text-muted);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.login-options :deep(.el-checkbox__label) {
  color: var(--text-secondary);
  font-size: 13px;
}

.forgot {
  font-size: 13px;
  color: var(--primary-text);
  cursor: pointer;
  transition: color 0.2s;
}

.forgot:hover {
  color: var(--primary);
}

.login-btn {
  width: 100%;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  border-radius: 10px;
  background: var(--primary-grad);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.3);
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, transparent 50%);
  pointer-events: none;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(176, 123, 62, 0.38);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(176, 123, 62, 0.3);
}

.login-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
  box-shadow: none;
}

.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  margin-top: 28px;
  text-align: center;
  font-size: 12px;
  color: var(--text-faint);
}
</style>
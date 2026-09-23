<template>
  <div class="daily-report-shell">
    <aside class="daily-sidebar">
      <div class="daily-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" fill="none" width="22" height="22">
            <path d="M12 2L4 5.5V11C4 16.5 7.5 20.5 12 22C16.5 20.5 20 16.5 20 11V5.5L12 2Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
            <path d="M9 11.5L11.2 13.7L15.5 9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="brand-copy">
          <h1>智能日报</h1>
          <p>建筑安全智能监控</p>
        </div>
      </div>

      <nav class="daily-nav">
        <p class="nav-label">功能菜单</p>
        <button
          v-for="item in menuItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeMenu === item.key }"
          type="button"
          @click="activeMenu = item.key"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/home" class="back-home">
          <svg viewBox="0 0 24 24" fill="none" width="16" height="16">
            <path d="M7 16L3 12M3 12L7 8M3 12H21" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回主页
        </router-link>
        <p>建筑安全智能监控平台 v1.0</p>
      </div>
    </aside>

    <main class="daily-main">
      <header class="daily-topbar">
        <nav class="daily-breadcrumb" aria-label="面包屑">
          <span>智能日报</span>
          <svg viewBox="0 0 20 20" fill="currentColor" width="12" height="12">
            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
          </svg>
          <strong>{{ currentBreadcrumb }}</strong>
        </nav>
        <span class="status-chip">
          <i></i>
          AI 服务已连接
        </span>
      </header>

      <section class="daily-content">
        <DailyReportAssistant v-if="activeMenu === 'assistant'" />
        <DailyReportKnowledgeBase v-else-if="activeMenu === 'knowledge'" />
        <DailyReportTemplateMng v-else-if="activeMenu === 'template'" />
        <DailyReportRuleMng v-else-if="activeMenu === 'rule'" />
        <DailyReportList v-else-if="activeMenu === 'list'" />
        <TaskPlaceholder
          v-else
          breadcrumb-root="智能日报"
          :page-title="currentBreadcrumb"
          :description="placeholderDescriptions[activeMenu]"
        />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import DailyReportAssistant from './DailyReportAssistant.vue'
import DailyReportKnowledgeBase from './DailyReportKnowledgeBase.vue'
import DailyReportTemplateMng from './DailyReportTemplateMng.vue'
import DailyReportRuleMng from './DailyReportRuleMng.vue'
import DailyReportList from './DailyReportList.vue'
import TaskPlaceholder from './TaskPlaceholder.vue'

const activeMenu = ref('assistant')

const menuItems = [
  {
    key: 'assistant',
    label: '监测助手',
    icon: '<svg viewBox="0 0 24 24" fill="none" width="19" height="19"><path d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'knowledge',
    label: '知识库配置',
    icon: '<svg viewBox="0 0 24 24" fill="none" width="19" height="19"><path d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'template',
    label: '日报模板配置',
    icon: '<svg viewBox="0 0 24 24" fill="none" width="19" height="19"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'rule',
    label: '日报规则配置',
    icon: '<svg viewBox="0 0 24 24" fill="none" width="19" height="19"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'list',
    label: '日报查看',
    icon: '<svg viewBox="0 0 24 24" fill="none" width="19" height="19"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  }
]

const breadcrumbLabels = {
  assistant: '监测助手',
  knowledge: '知识库配置',
  template: '日报模板配置',
  rule: '日报规则配置',
  list: '日报查看'
}

const placeholderDescriptions = {}

const currentBreadcrumb = computed(() => breadcrumbLabels[activeMenu.value] || '智能日报')
</script>

<style scoped>
.daily-report-shell {
  display: flex;
  width: 100%;
  height: 100vh;
  min-width: 960px;
  overflow: hidden;
  background: var(--bg-page);
  color: var(--text-main);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.daily-sidebar {
  display: flex;
  width: 224px;
  flex-shrink: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--bg-white);
  box-shadow: 4px 0 18px rgba(120, 95, 60, 0.05);
  z-index: 2;
}

.daily-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 21px 18px 18px;
  border-bottom: 1px solid var(--border);
}

.brand-icon {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 11px;
  background: var(--primary-grad);
  color: #fff;
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.22);
}

.brand-copy {
  min-width: 0;
}

.brand-copy h1 {
  margin: 0;
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.brand-copy p {
  margin: 3px 0 0;
  color: var(--text-muted);
  font-size: 11px;
}

.daily-nav {
  flex: 1;
  overflow-y: auto;
  padding: 17px 12px;
}

.nav-label {
  margin: 0 8px 8px;
  color: var(--text-faint);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
}

.nav-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: var(--bg-soft);
  color: var(--text-strong);
}

.nav-item.active {
  border-color: rgba(176, 123, 62, 0.18);
  background: var(--primary-tint);
  color: var(--primary-text);
  font-weight: 700;
  box-shadow: inset 3px 0 0 var(--primary);
}

.nav-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
}

.sidebar-footer {
  padding: 14px 16px 16px;
  border-top: 1px solid var(--border);
}

.back-home {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 9px;
  color: var(--text-secondary);
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s ease;
}

.back-home:hover {
  background: var(--bg-soft);
  color: var(--primary-text);
}

.sidebar-footer p {
  margin: 11px 0 0;
  color: var(--text-faint);
  font-size: 10px;
  text-align: center;
}

.daily-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.daily-topbar {
  display: flex;
  min-height: 62px;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
}

.daily-breadcrumb {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--text-muted);
  font-size: 12px;
}

.daily-breadcrumb strong {
  color: var(--text-main);
  font-weight: 700;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 10px;
  border: 1px solid rgba(77, 133, 71, 0.18);
  border-radius: 999px;
  background: var(--success-tint);
  color: var(--success);
  font-size: 11px;
}

.status-chip i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 8px rgba(77, 133, 71, 0.55);
}

.daily-content {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

@media (max-width: 1100px) {
  .daily-report-shell {
    min-width: 0;
  }

  .daily-sidebar {
    width: 194px;
  }

  .daily-brand {
    padding-right: 12px;
    padding-left: 12px;
  }

  .brand-copy p {
    display: none;
  }
}
</style>
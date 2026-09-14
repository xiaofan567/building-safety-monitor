<template>
  <div class="placeholder-page">
    <div class="breadcrumb">
      <span class="crumb-tag">{{ breadcrumbRoot }}</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ pageTitle }}</span>
    </div>
    <div class="placeholder-card">
      <div class="placeholder-emoji">🛠️</div>
      <h2>{{ pageTitle }}</h2>
      <p>{{ description }}</p>
      <span class="placeholder-tag">该模块正在开发中</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  breadcrumbRoot: { type: String, default: '施工环境' },
  pageTitle: { type: String, default: '功能开发中' },
  description: { type: String, default: '当前模块尚未完成开发，敬请期待。' }
})
</script>

<style scoped>
.placeholder-page {
  display: flex;
  min-height: 100%;
  flex: 1;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
}
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.placeholder-card {
  display: flex;
  min-height: 60vh;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  border: 1px dashed var(--border-strong);
  border-radius: 16px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  text-align: center;
}
.placeholder-emoji {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--primary-tint);
  font-size: 30px;
}
.placeholder-card h2 {
  margin: 16px 0 6px;
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}
.placeholder-card p {
  max-width: 480px;
  margin: 0 0 14px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}
.placeholder-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--primary-tint);
  color: var(--primary-text);
  font-size: 12px;
  font-weight: 600;
}
</style>

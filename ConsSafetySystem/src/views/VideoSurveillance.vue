<template>
  <div class="video-surveillance">
    <div class="breadcrumb">
      <span class="crumb-tag">施工场地</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">现场监控</span>
    </div>

    <div class="surveillance-layout">
      <!-- 左侧：摄像头列表 -->
      <aside class="camera-sidebar">
        <div class="sidebar-head">
          <h3>摄像头列表</h3>
          <span class="count-pill">{{ cameras.length }}</span>
        </div>
        <div class="camera-list">
          <div
            v-for="camera in cameras"
            :key="camera.id"
            class="camera-item"
            :class="{ active: selectedCameraId === camera.id }"
            @click="selectCamera(camera.id)"
          >
            <div class="camera-info">
              <span class="camera-name">{{ camera.assetName }}</span>
              <span class="camera-loc">{{ camera.locationName || '未定位' }}</span>
            </div>
            <span class="status-pill" :class="camera.status === '1' ? 'online' : 'offline'">
              {{ camera.status === '1' ? '在线' : '离线' }}
            </span>
          </div>
          <div v-if="!cameras.length" class="list-empty">暂无摄像头数据</div>
        </div>
      </aside>

      <!-- 右侧：视频播放区 -->
      <section class="video-area">
        <FlvLivePlayer
          ref="flvPlayerRef"
          :cameras="cameras"
          :selected-video="selectedCameraId"
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import FlvLivePlayer from '@/components/FlvLivePlayer.vue'
import { selectVideoData } from '@/api/video'

const cameras = ref([])
const selectedCameraId = ref(0)
const flvPlayerRef = ref(null)

async function loadData() {
  try {
    const res = await selectVideoData({ page: 1, limit: 100 })
    const records = (res && res.data && (res.data.records || res.data.list)) || []
    // 优先展示在线摄像头
    cameras.value = records.slice().sort((a, b) => Number(String(b.status === '1')) - Number(String(a.status === '1')))
  } catch (e) {
    console.error('摄像头数据加载失败', e)
  }
}

function selectCamera(cameraId) {
  selectedCameraId.value = cameraId
  const index = cameras.value.findIndex(c => c.id === cameraId)
  if (index !== -1 && flvPlayerRef.value) {
    flvPlayerRef.value.toggleFullscreen(index + 1)
  }
}

onMounted(loadData)
</script>

<style scoped>
.video-surveillance {
  height: 100%;
  display: flex;
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
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 12px;
}
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.surveillance-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 16px;
}

.camera-sidebar {
  display: flex;
  flex-direction: column;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
}
.sidebar-head h3 { margin: 0; font-size: 14px; font-weight: 600; color: var(--text-main); }
.count-pill {
  padding: 1px 10px;
  border-radius: 999px;
  background: var(--primary-tint);
  color: var(--primary-text);
  font-size: 12px;
  font-weight: 600;
}
.camera-list { flex: 1; overflow-y: auto; padding: 8px; }
.camera-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s;
  border: 1px solid transparent;
}
.camera-item:hover { background: var(--bg-hover); }
.camera-item.active { background: var(--primary-tint); border-color: rgba(176,123,62,.3); }
.camera-info { display: flex; flex-direction: column; min-width: 0; }
.camera-name { font-size: 13px; font-weight: 500; color: var(--text-main); }
.camera-loc { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.status-pill {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  flex-shrink: 0;
}
.status-pill.online { background: var(--success-tint); color: var(--success); }
.status-pill.offline { background: var(--danger-tint); color: var(--danger); }
.list-empty { padding: 30px; text-align: center; color: var(--text-muted); font-size: 13px; }

.video-area {
  min-width: 0;
  min-height: 0;
  padding: 16px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}
</style>
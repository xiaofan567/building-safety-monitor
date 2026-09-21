<template>
  <div class="player-panel">
    <div class="player-toolbar">
      <span class="toolbar-title">现场视频 · {{ cameras.length }} 路</span>
      <div class="toolbar-right">
        <button class="layout-btn" :class="{ active: gridCols === 2 }" @click="gridCols = 2">两分屏</button>
        <button class="layout-btn" :class="{ active: gridCols === 3 }" @click="gridCols = 3">三分屏</button>
        <button class="layout-btn" :class="{ active: gridCols === 4 }" @click="gridCols = 4">四分屏</button>
      </div>
    </div>

    <div class="player-grid" :class="gridClass">
      <div
        v-for="(camera, index) in cameras"
        :key="camera.id || index"
        class="player-cell"
        :class="{ 'player-cell--selected': (index + 1) === fullscreenPlayer }"
      >
        <div class="cell-head">
          <span class="cell-name">{{ camera.assetName || (`摄像头 ${index + 1}`) }}</span>
          <span class="cell-loc">{{ camera.locationName || '未定位' }}</span>
        </div>
        <div class="cell-body">
          <video
            :ref="el => setVideoRef(el, index)"
            class="video-element"
            :poster="cameraStreamPoster"
            autoplay
            muted
            playsinline
          ></video>
          <div class="loading-mask" v-show="loadingStates[index]">
            <div class="spinner"></div>
            <span>加载中…</span>
          </div>
          <div class="empty-mask" v-if="!loadingStates[index] && hasError[index]">
            <span>视频流暂不可用</span>
          </div>
          <button class="fullscreen-btn" title="全屏" @click="toggleFullscreen(index + 1)">
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
          </button>
        </div>
      </div>
      <div v-if="!cameras.length" class="grid-empty">
        <div class="spinner"></div>
        <span>正在加载摄像头列表…</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import Hls from 'hls.js'

const props = defineProps({
  cameras: { type: Array, default: () => [] },
  selectedVideo: { type: Number, default: 0 }
})

const gridCols = ref(2)
const videoRefs = ref([])
const loadingStates = ref([])
const hasError = ref([])
const isFullscreen = ref(false)
const fullscreenPlayer = ref(null)

// 浏览器不支持 video 元素 / 无可用视频源时的兜底
const cameraStreamPoster = ''

const gridClass = computed(() => {
  const n = Math.max(1, gridCols.value)
  return `grid-${n < 2 ? 2 : n}`
})

function setVideoRef(el, index) {
  videoRefs.value[index] = el
}

function getStreamUrl(camera) {
  const hlsBaseUrl = 'http://localhost:8068/hls'
  if (camera && camera.wvpOpenUrl) return `${hlsBaseUrl}/${camera.wvpOpenUrl}/index.m3u8`
  return `${hlsBaseUrl}/test1/index.m3u8`
}

let hlsInstances = []

function initHlsPlayer(videoElement, sourceUrl, index) {
  if (!videoElement) return

  if (Hls.isSupported()) {
    if (hlsInstances[index]) hlsInstances[index].destroy()
    const hls = new Hls({ debug: false, enableWorker: true, lowLatencyMode: true, backBufferLength: 90 })
    hlsInstances[index] = hls
    hls.loadSource(sourceUrl)
    hls.attachMedia(videoElement)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      videoElement.play().catch(() => {})
    })
    hls.on(Hls.Events.ERROR, (event, data) => {
      if (data && data.fatal) {
        hasError.value[index] = true
        if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
          setTimeout(() => hls.startLoad(), 3000)
        } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
          hls.recoverMediaError()
        }
      }
    })
  } else if (videoElement.canPlayType && videoElement.canPlayType('application/vnd.apple.mpegurl')) {
    videoElement.src = sourceUrl
    videoElement.addEventListener('canplay', () => videoElement.play().catch(() => {}))
  }
}

function initAllPlayers() {
  const cameras = props.cameras || []
  const count = cameras.length
  loadingStates.value = Array(count).fill(true)
  hasError.value = Array(count).fill(false)

  cameras.forEach((camera, index) => {
    nextTick(() => {
      const el = videoRefs.value[index]
      if (!el) return
      initHlsPlayer(el, getStreamUrl(camera), index)
      el.addEventListener('waiting', () => { loadingStates.value[index] = true })
      el.addEventListener('playing', () => { loadingStates.value[index] = false })
    })
  })
}

watch(() => props.cameras, (list) => {
  if (!list || !list.length) return
  initAllPlayers()
}, { immediate: false, deep: true })

function toggleFullscreen(playerId) {
  const videoElement = videoRefs.value[playerId - 1]
  if (!videoElement) return

  if (!isFullscreen.value) {
    const req = videoElement.requestFullscreen || videoElement.webkitRequestFullscreen || videoElement.msRequestFullscreen
    if (req) req.call(videoElement)
    isFullscreen.value = true
    fullscreenPlayer.value = playerId
  } else {
    const exit = document.exitFullscreen || document.webkitExitFullscreen || document.msExitFullscreen
    if (exit) exit.call(document)
    isFullscreen.value = false
    fullscreenPlayer.value = null
  }
}

defineExpose({ toggleFullscreen })

const handleFullscreenChange = () => {
  if (!document.fullscreenElement && !document.webkitFullscreenElement && !document.msFullscreenElement) {
    isFullscreen.value = false
    fullscreenPlayer.value = null
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('msfullscreenchange', handleFullscreenChange)
  if (props.cameras && props.cameras.length) nextTick(initAllPlayers)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('msfullscreenchange', handleFullscreenChange)
  hlsInstances.forEach(instance => { if (instance) instance.destroy() })
})
</script>

<style scoped>
.player-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.player-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}
.toolbar-title { font-size: 14px; font-weight: 600; color: var(--text-main); }
.toolbar-right { display: flex; gap: 8px; }
.layout-btn {
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
}
.layout-btn:hover { border-color: var(--primary); color: var(--primary); }
.layout-btn.active { background: var(--primary-grad); color: #fff; border-color: transparent; }

.player-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  gap: 12px;
  overflow-y: auto;
}
.player-grid.grid-2 { grid-template-columns: repeat(2, 1fr); }
.player-grid.grid-3 { grid-template-columns: repeat(3, 1fr); }
.player-grid.grid-4 { grid-template-columns: repeat(2, 1fr); }

.player-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 240px;
  border-radius: 10px;
  overflow: hidden;
  background: #17140f;
  border: 1px solid var(--border);
}
.player-cell--selected { border-color: var(--primary); box-shadow: 0 0 0 1px var(--primary); }

.cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: linear-gradient(180deg, #2b241a, #1d1812);
  color: #e8dcc8;
  font-size: 12px;
  flex-shrink: 0;
}
.cell-name { font-weight: 600; }
.cell-loc { color: #a89a82; font-size: 11px; }

.cell-body {
  position: relative;
  flex: 1;
  min-height: 0;
  background: #000;
}
.video-element {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #000;
}
.loading-mask, .empty-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #cfc2ab;
  font-size: 13px;
  background: rgba(12, 10, 7, 0.72);
}
.spinner {
  width: 26px;
  height: 26px;
  border: 3px solid rgba(201, 162, 107, .25);
  border-top-color: #c9a26b;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.fullscreen-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 5;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
  transition: background .2s;
}
.fullscreen-btn:hover { background: rgba(0, 0, 0, 0.8); }

.grid-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 320px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
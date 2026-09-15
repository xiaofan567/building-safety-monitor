<template>
  <div class="flex-[3] relative mt-2 mr-5 ml-5 panel" ref="mapEl">
    <!-- 项目平面图 -->
    <img
      :onerror="`this.src='${noImgSrc}'`"
      src="../assets/img/jixiezonglan.png"
      alt="项目平面图"
      class="w-full h-auto block select-none"
    />
    <!-- 设备图标列表（带tooltip；编辑态可拖动，松手自动保存） -->
    <div v-for="item in elevators" :key="item.assetId">
      <el-tooltip class="item" effect="dark" :content="item.assetName" :disabled="editable" placement="top">
        <!-- 升降机图标 (typeId=115) -->
        <img
          v-if="item.planeCoordinatePc && item.typeId == 115"
          class="dtimg absolute w-[60px] h-[60px] z-10 eq-icon"
          :class="{ editable }"
          :style="iconStyle(item)"
          :draggable="false"
          @mousedown.prevent="editable && startDrag($event, item)"
          @click="!editable && goStateList(item.assetId, item.monitorType, item.assetName)"
          src="../assets/img/zlsjjico.png"
          alt="升降机图标"
        />
        <!-- 塔机图标 (typeId=145，monitorType=2) -->
        <img
          v-if="item.planeCoordinatePc && item.typeId == 145"
          class="dtimg absolute w-[60px] h-[60px] z-10 eq-icon"
          :class="{ editable }"
          :style="iconStyle(item)"
          :draggable="false"
          @mousedown.prevent="editable && startDrag($event, item)"
          @click="!editable && goStateList(item.assetId, item.monitorType, item.assetName)"
          src="../assets/img/tjdjimg.png"
          alt="塔机图标"
        />
        <!-- 高支模图标 (typeId=147，monitorType=4) -->
        <img
          v-if="item.planeCoordinatePc && item.typeId == 147"
          class="dtimg absolute w-[60px] h-[60px] z-10 eq-icon"
          :class="{ editable }"
          :style="iconStyle(item)"
          :draggable="false"
          @mousedown.prevent="editable && startDrag($event, item)"
          @click="!editable && goStateList(item.assetId, item.monitorType, item.assetName)"
          src="../assets/img/gzm_icon.png"
          alt="高支模图标"
        />
        <!-- 深基坑图标 (typeId=148，monitorType=1) -->
        <img
          v-if="item.planeCoordinatePc && item.typeId == 148"
          class="dtimg absolute w-[60px] h-[60px] z-10 eq-icon"
          :class="{ editable }"
          :style="iconStyle(item)"
          :draggable="false"
          @mousedown.prevent="editable && startDrag($event, item)"
          @click="!editable && goStateList(item.assetId, item.monitorType, item.assetName)"
          src="../assets/img/sjk_icon.png"
          alt="深基坑图标"
        />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import noImgSrc from '@/assets/noimgsrc.png'
import { saveEquipmentAssets } from '@/api/equipmentAssets'

const props = defineProps({
  elevators: { type: Array, default: () => [] },
  editable: { type: Boolean, default: false }
})
const emit = defineEmits(['goStateList'])

const mapEl = ref(null)
const scaleX = ref(1)
const scaleY = ref(1)

/* 缩放系数：库里坐标为原图像素，需按当前显示宽度换算 */
const computeScale = () => {
  const el = mapEl.value
  if (!el) return
  const img = el.querySelector('img')
  const rect = el.getBoundingClientRect()
  const nw = img ? img.naturalWidth : 0
  const nh = img ? img.naturalHeight : 0
  if (nw && rect.width) scaleX.value = rect.width / nw
  if (nh && rect.height) scaleY.value = rect.height / nh
}

const iconStyle = (item) => {
  const parts = (item.planeCoordinatePc || '').split(/[, ]+/)
  return {
    left: `${(parseFloat(parts[0]) || 0) * scaleX.value}px`,
    top: `${(parseFloat(parts[1]) || 0) * scaleY.value}px`,
    width: '50px'
  }
}

const goStateList = (assetId, monitorType, assetName) => {
  emit('goStateList', assetId, monitorType, assetName)
}

/* ---------------- 拖拽定位编辑 ---------------- */
let dragState = null

const startDrag = (event, item) => {
  computeScale()
  const parts = (item.planeCoordinatePc || '').split(/[, ]+/)
  dragState = {
    item,
    startClientX: event.clientX,
    startClientY: event.clientY,
    startX: (parseFloat(parts[0]) || 0) * scaleX.value,
    startY: (parseFloat(parts[1]) || 0) * scaleY.value
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

const onMove = (event) => {
  if (!dragState) return
  const ndx = dragState.startX + (event.clientX - dragState.startClientX)
  const ndy = dragState.startY + (event.clientY - dragState.startClientY)
  // 换算回原图像素并写回，触发渲染
  dragState.item.planeCoordinatePc = `${Math.round(ndx / scaleX.value)},${Math.round(ndy / scaleY.value)}`
}

const onUp = async () => {
  if (!dragState) return
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', onUp)
  const item = dragState.item
  dragState = null
  try {
    await saveEquipmentAssets({ ...item, planeCoordinatePc: item.planeCoordinatePc })
    ElMessage.success(`【${item.assetName}】位置已保存`)
  } catch (e) {
    ElMessage.error('位置保存失败')
  }
}

const cleanup = () => {
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', onUp)
}

onMounted(() => {
  computeScale()
  window.addEventListener('resize', computeScale)
})
onUnmounted(() => {
  cleanup()
  window.removeEventListener('resize', computeScale)
})
</script>

<style scoped>
.panel {
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
  background: var(--bg-white);
}
.eq-icon { cursor: pointer; }
.eq-icon.editable {
  cursor: move;
  outline: 2px dashed var(--primary);
  outline-offset: -2px;
  box-shadow: 0 0 6px rgba(180, 130, 60, 0.35);
}
</style>
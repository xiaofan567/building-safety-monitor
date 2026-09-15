<template>
  <div class="w-full h-full flex">
    <!-- 左侧平面图（共享组件，支持定位微调） -->
    <OverviewMap :elevators="elevators" :editable="editable" @goStateList="goStateList" />

    <!-- 右侧数据统计区域 -->
    <div class="flex-1 flex flex-col">
      <div class="flex-[0_0_auto] mt-2 mb-4 panel">
        <div class="flex items-center text-[var(--text-strong)] text-lg mt-2.5 ml-1">
          <img :onerror="`this.src='${noImgSrc}'`" src="../assets/img/Symbol.png" alt="图标" class="mr-2.5" />
          今日塔吊工作情况
        </div>
        <div class="mt-2">
          <!-- 在线设备统计 -->
          <div class="flex items-center mb-2 p-2 border-b border-[var(--border)]">
            <div class="flex-shrink-0">
              <img src="../assets/img/xzimg.png" alt="在线设备图标" class="w-10 h-10 object-contain" />
            </div>
            <div class="flex-1 text-center">
              <p class="text-base font-bold mb-0.5">{{ overviewData.equipmentAssetsOnlineCount }}</p>
              <p class="text-[var(--text-secondary)] text-sm">在线设备</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-[var(--text-secondary)] text-sm mt-3">设备总数</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-base font-bold mt-3">{{ overviewData.equipmentAssetsCount }}</p>
            </div>
          </div>

          <!-- 报警总数统计 -->
          <div class="flex items-center mb-2 p-2 border-b border-[var(--border)]">
            <div class="flex-shrink-0">
              <img :onerror="`this.src='${noImgSrc}'`" src="../assets/img/bjimg.png" alt="报警图标" class="w-10 h-10 object-contain" />
            </div>
            <div class="flex-1 text-center">
              <p class="text-base font-bold mb-0.5">{{ overviewData.alarmCount }}</p>
              <p class="text-[var(--text-secondary)] text-sm">报警总数</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-[var(--text-secondary)] text-sm">昨日数据</p>
              <p class="text-[var(--text-secondary)] text-sm mt-1">同比过去30日</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-[var(--text-muted)] mt-1.5">{{ overviewData.alarmYesCount }}</p>
              <p class="text-[var(--danger)] mt-1.5">{{ overviewData.alarmGrowth }}</p>
            </div>
          </div>

          <!-- 预警总数统计 -->
          <div class="flex items-center p-2">
            <div class="flex-shrink-0">
              <img :onerror="`this.src='${noImgSrc}'`" src="../assets/img/yjimg.png" alt="预警图标" class="w-10 h-10 object-contain" />
            </div>
            <div class="flex-1 text-center">
              <p class="text-base font-bold mb-0.5">{{ overviewData.warnCount }}</p>
              <p class="text-[var(--text-secondary)] text-sm">预警总数</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-[var(--text-secondary)] text-sm">昨日数据</p>
              <p class="text-[var(--text-secondary)] text-sm mt-1">同比过去30日</p>
            </div>
            <div class="flex-1 text-center">
              <p class="text-[var(--text-muted)] mt-1.5">{{ overviewData.warnYesCount }}</p>
              <p class="text-[var(--danger)] mt-1.5">{{ overviewData.warnGrowth }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 设备实时运行列表 -->
      <div class="flex-1 mt-1 panel">
        <div class="flex items-center text-[var(--text-strong)] text-lg ml-1 mt-2.5">
          <img :onerror="`this.src='${noImgSrc}'`" src="../assets/img/Symbol.png" alt="图标" class="mr-2.5" />
          设备实时运行列表
        </div>
        <div class="p-2.5 w-full mt-2">
          <div class="max-h-[300px] overflow-y-auto">
            <table class="w-full border-collapse">
              <tr v-for="item in elevators.slice(0, 7)" :key="item.assetId" class="even:bg-[var(--bg-muted)]">
                <td class="w-[30%] h-12 pl-2.5 border-0">{{ item.assetName }}</td>
                <td class="h-12 pl-2.5 border-0">{{ item.offlineDate }}</td>
                <td class="w-[35px] h-12 pl-2.5 border-0">
                  <img v-if="item.status == 1" height="10px" src="../assets/img/yxico.png" alt="运行图标" />
                  <img v-else height="10px" src="../assets/img/lxico.png" alt="离线图标" />
                </td>
                <td class="w-[60px] h-12 pl-2.5 border-0">
                  {{ item.status == 1 ? '  运行' : '  离线' }}
                </td>
              </tr>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import noImgSrc from '@/assets/noimgsrc.png'
import OverviewMap from './OverviewMap.vue'
import { selectEquipDataByType } from '@/api/equipment'

const TYPE_ID = 145 // 塔吊

const props = defineProps({
  editable: { type: Boolean, default: false }
})

const emit = defineEmits(['deviceClick'])

const elevators = ref([])
const overviewData = reactive({
  alarmCount: '',
  alarmYesCount: '',
  alarmGrowth: '',
  warnCount: '',
  warnYesCount: '',
  warnGrowth: '',
  equipmentAssetsOnlineCount: '',
  equipmentAssetsCount: ''
})

const init = async () => {
  try {
    const response = await selectEquipDataByType(TYPE_ID)
    const data = response?.data || {}
    overviewData.alarmCount = data.alarmCount?.compare_day_count ?? ''
    overviewData.alarmYesCount = data.alarmCount?.yesterday_count ?? ''
    overviewData.alarmGrowth = data.alarmCount?.growth_rate ?? ''
    overviewData.warnCount = data.warnCount?.compare_day_count ?? ''
    overviewData.warnYesCount = data.warnCount?.yesterday_count ?? ''
    overviewData.warnGrowth = data.warnCount?.growth_rate ?? ''
    overviewData.equipmentAssetsOnlineCount = data.equipmentAssetsOnlineCount
    overviewData.equipmentAssetsCount = data.equipmentAssetsCount
    elevators.value = data.equipmentAssetsList
  } catch (error) {
    console.error('初始化数据失败：', error)
  }
}

const goStateList = (assetId, monitorType, assetName) => {
  // 塔吊点击：上抛给父页面打开塔机监测分析
  if (monitorType === 2) {
    emit('deviceClick', { assetId, monitorType, assetName })
    return
  }
  let targetPath = ''
  let queryParams = { assetId }
  switch (monitorType) {
    case 2: targetPath = 'tower'; break
    case 3: targetPath = 'elevator'; break
    case 99: targetPath = 'hookBlindVisual'; queryParams = { ...queryParams, name: assetName, isHook: true }; break
    case 1: targetPath = 'foundationView'; break
    case 11: targetPath = 'highFormView'; break
    default:
      console.warn(`未匹配的设备类型：${monitorType}`)
      return
  }
  console.log('跳转目标：', targetPath, queryParams)
  // goToUrl(targetPath, queryParams)
}

let timer = null
onMounted(() => {
  init()
  timer = setInterval(init, 1800000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.panel {
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
  background: var(--bg-white);
}
</style>
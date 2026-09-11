<template>
  <div class="min-h-screen ov-wrap">
    <div class="flex items-center justify-between mt-2">
      <h2 class="text-l font-medium ov-breadcrumb"> 施工设备 > 机械设备总览 </h2>
      <el-button
        size="small"
        :type="editable ? 'primary' : 'default'"
        :plain="!editable"
        @click="editable = !editable"
      >
        {{ editable ? '退出编辑' : '编辑定位' }}
      </el-button>
    </div>

    <!-- 主体：左侧导航 + 右侧动态内容 -->
    <div class="flex h-full">
      <!-- 左侧设备类型导航 -->
      <div class="nat_dom_l">
        <div :class="type === 5 ? 'nat_but nat_but_xz' : 'nat_but'" @click="equipmentSwitch(5)">
          <img class="nat_but_img" :src="type === 5 ? zlimgb : zlimg1">
          <p class="nat_but_text">总览</p>
        </div>
        <div :class="type === 1 ? 'nat_but nat_but_xz' : 'nat_but'" @click="equipmentSwitch(1)">
          <img class="nat_but_img" :src="type === 1 ? tdanb : tdanl">
          <p class="nat_but_text">塔吊</p>
        </div>
        <div :class="type === 2 ? 'nat_but nat_but_xz' : 'nat_but'" @click="equipmentSwitch(2)">
          <img class="nat_but_img" :src="type === 2 ? sjjanb : sjjanl">
          <p class="nat_but_text">升降机</p>
        </div>
        <div :class="type === 3 ? 'nat_but nat_but_xz' : 'nat_but'" @click="equipmentSwitch(3)">
          <img class="nat_but_img" :src="type === 3 ? sjkanb : sjkanl">
          <p class="nat_but_text">深基坑</p>
        </div>
        <div :class="type === 4 ? 'nat_but nat_but_xz' : 'nat_but'" @click="equipmentSwitch(4)">
          <img class="nat_but_img" :src="type === 4 ? gzmanb : gzmanl">
          <p class="nat_but_text">高支模</p>
        </div>
      </div>

      <!-- 右侧动态组件 -->
      <component
        :is="type === 1 ? TowerCraneOverview
          : type === 2 ? ElevatorOverview
          : type === 3 ? DeepFoundationPitOverview
          : type === 4 ? HighFormworkSupportOverview
          : allOverview"
        :editable="editable"
      ></component>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import zlimgb from '../assets/img/zlimgb.png'     // 总览1
import zlimg1 from '../assets/img/zlimg1.png'     // 总览2
import tdanl from '../assets/img/tdanl.png'       // 塔吊1
import tdanb from '../assets/img/tdanb.png'       // 塔吊2
import sjkanb from '../assets/img/sjkanb.png'     // 深基坑1
import sjkanl from '../assets/img/sjkanl.png'     // 深基坑2
import sjjanb from '../assets/img/sjjanb.png'     // 升降机1
import sjjanl from '../assets/img/sjjanl.png'     // 升降机2
import gzmanb from '../assets/img/gzmanb.png'     // 高支模1
import gzmanl from '../assets/img/gzmanl.png'     // 高支模2

import TowerCraneOverview from '../components/TowerCraneOverview.vue'
import ElevatorOverview from '../components/ElevatorOverview.vue'
import DeepFoundationPitOverview from '../components/DeepFoundationPitOverview.vue'
import HighFormworkSupportOverview from '../components/HighFormworkSupportOverview.vue'
import allOverview from '../components/allOverview.vue'

// 初始类型（由顶部「施工设备」下拉传入：5总览 1塔吊 2升降机 3深基坑 4高支模）
const props = defineProps({
  initialType: { type: Number, default: 5 }
})

// 默认显示总览
const type = ref(props.initialType)

// 是否处于「编辑定位」状态（可拖动图标微调坐标，松手自动保存）
const editable = ref(false)

// 顶部下拉切换类型时联动
watch(
  () => props.initialType,
  (val) => {
    if (val) type.value = val
  }
)

const equipmentSwitch = (val) => {
  type.value = val
}

defineExpose({ type, equipmentSwitch })
</script>

<style scoped>
.ov-wrap { overflow: auto; }
.ov-breadcrumb { color: var(--text-muted); }

.nat_dom_l {
  display: inline-block;
  width: 69px;
  float: left;
  margin-right: 20px;
  height: 500px;
  margin-top: 10px;
  flex-shrink: 0;
}

.nat_but {
  margin: 10px;
  padding: 4% 0 19%;
  text-align: center;
  width: 100%;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.nat_but p { color: var(--primary-text); }

.nat_but:hover { background: var(--bg-hover); }

.nat_but_img {
  width: 24px;
  height: 24px;
  margin: 14px 22px 0;
}

.nat_but_text {
  height: 17px;
  font-weight: 400;
}

.nat_but_xz {
  background-color: var(--primary);
  color: #ffffff;
}
.nat_but_xz p { color: white; }
</style>
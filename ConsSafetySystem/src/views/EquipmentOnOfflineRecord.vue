<template>
  <div class="eon-page">
    <h2 class="eon-crumb">施工设备 > 设备上下线记录</h2>

    <!-- 筛选区 -->
    <div class="eon-panel">
      <div class="eon-filter-grid">
        <div>
          <label class="eon-label">设备ID</label>
          <input
            v-model.trim="filters.equipmentId"
            type="text"
            placeholder="请输入设备ID"
            class="eon-input"
          />
        </div>
        <div>
          <label class="eon-label eon-label-empty">操作</label>
          <div class="eon-ops">
            <button class="eon-btn eon-btn-primary" @click="searchAlarms">搜索</button>
            <button class="eon-btn eon-btn-plain" @click="resetFilters">重置</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="eon-panel eon-table-panel">
      <div class="eon-table-wrap">
        <table class="eon-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>设备名称</th>
              <th>设备编号</th>
              <th>设备类型</th>
              <th>监测时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in equipmentData" :key="item.id">
              <td>{{ (pagination.currentPage - 1) * pagination.pageSize + index + 1 }}</td>
              <td>{{ item.equipmentName || '-' }}</td>
              <td>{{ item.equipmentCode || '-' }}</td>
              <td>{{ item.equipmentType || '-' }}</td>
              <td>{{ item.monitorTime }}</td>
              <td>
                <span class="status-tag" :class="item.status === '在线' ? 'status-on' : 'status-off'">
                  {{ item.status }}
                </span>
              </td>
            </tr>
            <tr v-if="equipmentData.length === 0">
              <td colspan="6" class="eon-empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="eon-pagination">
        <div class="eon-page-info">
          <span>共 {{ totalItems }} 条</span>
          <span>{{ pagination.pageSize }} 条/页</span>
        </div>
        <div class="eon-page-btns">
          <button class="eon-page-btn" @click="prevPage" :disabled="pagination.currentPage <= 1">‹</button>
          <button
            v-for="page in visiblePages"
            :key="page"
            class="eon-page-num"
            :class="{ 'eon-page-on': page === pagination.currentPage }"
            @click="goToPage(page)"
          >{{ page }}</button>
          <button class="eon-page-btn" @click="nextPage" :disabled="pagination.currentPage >= pagination.totalPages">›</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { selectOnOffLineData } from '@/api/equipment'

const filters = ref({ equipmentId: '' })

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})

const equipmentData = ref([])

const totalItems = computed(() => pagination.total)

const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, pagination.currentPage - 2)
  const end = Math.min(pagination.totalPages, pagination.currentPage + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// history.go(-1) 风控无；格式化后端 LocalDateTime
function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const loadEquipmentData = async () => {
  try {
    const payload = {
      page: pagination.currentPage,
      limit: pagination.pageSize
    }
    const equipmentId = Number(filters.value.equipmentId)
    if (filters.value.equipmentId && !Number.isNaN(equipmentId)) {
      payload.equipmentId = equipmentId
    }

    // 本项目 request 拦截器已解包 Result，这里取 .data 得到 BPage
    const resp = await selectOnOffLineData(payload)
    const bp = resp && resp.data ? resp.data : {}

    equipmentData.value = (bp.records || []).map((item) => ({
      id: item.id,
      equipmentName: item.equipmentName || '',
      equipmentCode: item.assetCode || item.equipmentId || '',
      equipmentType: item.typeName || '',
      monitorTime: formatTime(item.monitorTime),
      status: item.status === 1 ? '在线' : '离线'
    }))
    pagination.total = bp.total || 0
    pagination.totalPages = Math.max(1, bp.pages || Math.ceil(pagination.total / pagination.pageSize))
  } catch (error) {
    console.error('加载设备上下线数据失败:', error)
    equipmentData.value = []
    pagination.total = 0
    pagination.totalPages = 0
  }
}

const searchAlarms = () => { pagination.currentPage = 1; loadEquipmentData() }

const resetFilters = () => { filters.value = { equipmentId: '' }; pagination.currentPage = 1; loadEquipmentData() }

const goToPage = (page) => {
  if (page < 1 || page > pagination.totalPages) return
  pagination.currentPage = page
  loadEquipmentData()
}

const prevPage = () => { if (pagination.currentPage > 1) { pagination.currentPage--; loadEquipmentData() } }

const nextPage = () => { if (pagination.currentPage < pagination.totalPages) { pagination.currentPage++; loadEquipmentData() } }

onMounted(loadEquipmentData)
</script>

<style scoped>
.eon-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 16px;
}

.eon-crumb {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  margin: 8px 0 12px;
}

.eon-panel {
  background: var(--bg-white, #fff);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
}

.eon-filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  padding: 20px 24px;
  align-items: end;
}

.eon-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.eon-label-empty { opacity: 0; }

.eon-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-strong);
  background: var(--bg-white, #fff);
  outline: none;
}
.eon-input:focus { border-color: var(--primary-text); box-shadow: 0 0 0 2px var(--primary-grad); }

.eon-ops { display: flex; gap: 12px; }

.eon-btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}
.eon-btn-primary {
  background: var(--primary-grad);
  color: #fff;
  border: none;
}
.eon-btn-primary:hover { box-shadow: 0 4px 12px rgba(0,0,0,.12); }
.eon-btn-plain {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.eon-btn-plain:hover { background: var(--bg-muted); }

.eon-table-panel { padding: 0; overflow: hidden; }

.eon-table-wrap { overflow-x: auto; }

.eon-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}
.eon-table th {
  text-align: left;
  padding: 12px 18px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: .03em;
  background: var(--bg-muted);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.eon-table td {
  padding: 12px 18px;
  font-size: 13px;
  color: var(--text-strong);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.eon-table tbody tr:hover { background: var(--bg-muted); }

.eon-empty { text-align: center; color: var(--text-muted); padding: 32px 0 !important; }

.status-tag {
  display: inline-flex;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
}
.status-on { background: rgba(46,125,50,.12); color: #2e7d50; }
.status-off { background: rgba(211,47,47,.12); color: #d32f2f; }

.eon-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--border);
  background: var(--bg-muted);
}
.eon-page-info { display: flex; gap: 16px; font-size: 13px; color: var(--text-secondary); }
.eon-page-btns { display: flex; align-items: center; gap: 4px; }
.eon-page-btn, .eon-page-num {
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-white, #fff);
  cursor: pointer;
}
.eon-page-btn:disabled { opacity: .5; cursor: not-allowed; }
.eon-page-num:hover { border-color: var(--primary-text); }
.eon-page-on { background: var(--primary-grad); border-color: transparent; color: #fff; }
</style>
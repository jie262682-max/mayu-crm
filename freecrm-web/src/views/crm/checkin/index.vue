<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;align-items:center;gap:8px">
      <span style="color:#999;font-size:13px">移动端客户详情「到场签到」自动打卡并生成上门跟进；下为签到轨迹</span>
    </div>
    <el-table :data="rows" border size="small">
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="客户" min-width="160">
        <template #default="{ row }">{{ custName(row.customerId) }}</template>
      </el-table-column>
      <el-table-column label="签到人" width="130">
        <template #default="{ row }">{{ userName(row.userId) }}</template>
      </el-table-column>
      <el-table-column label="距客户" width="120">
        <template #default="{ row }">
          <span v-if="row.distance != null" :style="{ color: row.distance <= 500 ? '#67C23A' : '#E6A23C' }">{{ row.distance }} 米</span>
          <span v-else style="color:#999">—</span>
        </template>
      </el-table-column>
      <el-table-column label="坐标" width="190">
        <template #default="{ row }">{{ row.lng }}, {{ row.lat }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const users = ref([]); const customers = ref([])

onMounted(async () => {
  const [us, cs] = await Promise.all([
    http.get('/api/user/simple-list').catch(() => []),
    http.get('/api/crm/customer/page', { params: { pageNo: 1, pageSize: 500 } }).catch(() => ({}))
  ])
  users.value = us || []
  customers.value = cs.records || []
  load()
})

async function load() {
  const p = await http.get('/api/crm/checkin/page', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records || []; total.value = p.total || 0
}

const userName = id => { const u = users.value.find(x => x.id === id); return u ? u.nickname : ('用户' + id) }
const custName = id => { const c = customers.value.find(x => x.id === id); return c ? c.name : ('客户' + id) }
</script>

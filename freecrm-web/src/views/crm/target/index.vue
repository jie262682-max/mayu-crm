<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px">
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" style="width:140px" @change="load" />
      <span style="color:#999;font-size:13px">实际完成 = 当月已审合同签约额（按下单日期）；目标为 0 或空 = 不设目标</span>
      <el-button type="primary" style="margin-left:auto" @click="save">保存本月目标</el-button>
    </div>
    <el-table :data="rows" border size="small">
      <el-table-column label="用户" prop="nickname" width="140" />
      <el-table-column label="目标金额（元）" width="220">
        <template #default="{ row }">
          <el-input-number v-model="row.amount" :min="0" :precision="2" :controls="false" style="width:200px" placeholder="不设目标" />
        </template>
      </el-table-column>
      <el-table-column label="实际完成（元）" prop="actual" width="140" />
      <el-table-column label="完成率" width="200">
        <template #default="{ row }">
          <el-progress :percentage="pct(row)" :stroke-width="14" :color="pctColor(row)" />
        </template>
      </el-table-column>
      <el-table-column label="备注">
        <template #default="{ row }">
          <el-input v-model="row.remark" size="small" placeholder="备注" />
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../../api/request'

const month = ref(new Date().toISOString().slice(0, 7))
const rows = ref([])

onMounted(load)

async function load() {
  const [list, users] = await Promise.all([
    http.get('/api/crm/target/list', { params: { month: month.value } }),
    http.get('/api/user/simple-list')
  ])
  const byUser = Object.fromEntries((list || []).map(t => [t.userId, t]))
  rows.value = (users || []).map(u => {
    const t = byUser[u.id] || {}
    return { userId: u.id, nickname: u.nickname, amount: t.target ?? null, actual: t.actual ?? 0, rate: t.rate ?? null, remark: t.remark ?? '' }
  })
}

async function save() {
  await http.post('/api/crm/target/save', {
    month: month.value,
    items: rows.value.filter(r => r.amount && r.amount > 0).map(r => ({ userId: r.userId, amount: r.amount, remark: r.remark }))
  })
  ElMessage.success('已保存')
  load()
}

function pct(row) {
  if (!row.amount || row.amount <= 0) return 0
  return Math.min(100, Math.round((row.actual / row.amount) * 100))
}
function pctColor(row) {
  const p = row.amount > 0 ? (row.actual / row.amount) * 100 : 0
  return p >= 100 ? '#67C23A' : p >= 60 ? '#409EFF' : '#E6A23C'
}
</script>

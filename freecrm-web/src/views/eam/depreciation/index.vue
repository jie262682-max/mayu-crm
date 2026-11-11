<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="ym" placeholder="月份 yyyy-MM" style="width:150px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="generate">手动生成上月折旧</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="assetId" label="资产ID" width="80" />
      <el-table-column prop="depMonth" label="月份" width="100" />
      <el-table-column prop="amount" label="本月折旧" width="120" />
      <el-table-column prop="netValue" label="期末净值" width="120" />
      <el-table-column prop="createTime" label="生成时间" width="170" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const ym = ref('')

async function load() {
  const p = await http.get('/api/eam/depreciation/page', {
    params: { pageNo: pageNo.value, pageSize: 10, depMonth: ym.value || undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
async function generate() {
  const n = await http.post('/api/eam/depreciation/generate', {})
  ElMessage.success(`已生成 ${n} 条折旧记录（上月，幂等）`)
  load()
}
onMounted(load)
</script>

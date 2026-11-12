<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-select v-model="typeF" placeholder="变更类型" clearable style="width:140px" @change="load">
        <el-option v-for="(n, i) in ['', '领用', '退回', '借出', '归还', '维修', '报废']" v-if="i > 0" :key="i" :label="n" :value="i" />
      </el-select>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="assetId" label="资产ID" width="80" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ ['', '领用', '退回', '借出', '归还', '维修', '报废'][row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="从" width="100">
        <template #default="{ row }">{{ row.fromUserId ? (userMap[row.fromUserId] || row.fromUserId) : '-' }}</template>
      </el-table-column>
      <el-table-column label="到" width="100">
        <template #default="{ row }">{{ row.toUserId ? (userMap[row.toUserId] || row.toUserId) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="160" />
      <el-table-column prop="changeTime" label="变更时间" width="170" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const typeF = ref(null)
const userMap = ref({})
async function loadUsers() {
  const list = await http.get('/api/user/simple-list')
  const m = {}
  ;(list || []).forEach(u => { m[u.id] = u.nickname })
  userMap.value = m
}

async function load() {
  const p = await http.get('/api/eam/asset/change/page', {
    params: { pageNo: pageNo.value, pageSize: 10, type: typeF.value ?? undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
onMounted(() => { loadUsers(); load() })
</script>

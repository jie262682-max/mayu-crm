<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="客户名称" style="width:220px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="客户名称" min-width="150" />
      <el-table-column prop="mobile" label="手机" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="receive(row)">领取</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')

async function load() {
  const p = await http.get('/api/crm/customer/page', { params: { pageNo: pageNo.value, pageSize: 10, pool: true, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
async function receive(row) {
  await http.put(`/api/crm/customer/receive?id=${row.id}`)
  ElMessage.success(`已领取「${row.name}」`)
  load()
}
onMounted(load)
</script>

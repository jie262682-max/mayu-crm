<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-table :data="rows" border stripe size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorMsg" label="错误信息" min-width="150" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="createTime" label="时间" width="170" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="15" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)

async function load() {
  const p = await http.get('/api/log/login/page', { params: { pageNo: pageNo.value, pageSize: 15 } })
  rows.value = p.records; total.value = Number(p.total)
}
onMounted(load)
</script>

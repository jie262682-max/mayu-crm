<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="模块/操作/用户名" style="width:240px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" border stripe size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="module" label="模块" width="110" />
      <el-table-column prop="action" label="操作" width="140" />
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时(ms)" width="85" />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column prop="errorMsg" label="错误" min-width="120" />
      <el-table-column prop="createTime" label="时间" width="165" />
      <el-table-column type="expand">
        <template #default="{ row }">
          <pre style="margin:4px;font-size:12px">{{ row.method }}\n{{ row.params }}</pre>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="15" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')

async function load() {
  const p = await http.get('/api/log/oper/page', { params: { pageNo: pageNo.value, pageSize: 15, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
onMounted(load)
</script>

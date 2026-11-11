<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="任务ID" width="80" />
      <el-table-column prop="instanceId" label="流程实例" width="90" />
      <el-table-column prop="nodeLevel" label="节点" width="70" />
      <el-table-column prop="createTime" label="到达时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="success" @click="audit(row, true)">通过</el-button>
          <el-button link type="danger" @click="audit(row, false)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])

async function load() {
  rows.value = await http.get('/api/oa/flow/todo')
}
async function audit(row, pass) {
  const { value } = await ElMessageBox.prompt('审批意见（可空）', pass ? '通过' : '驳回', {
    inputType: 'textarea'
  }).catch(() => ({ value: undefined }))
  if (value === undefined) return
  await http.put(`/api/oa/flow/audit?taskId=${row.id}&pass=${pass}&opinion=${encodeURIComponent(value || '')}`)
  ElMessage.success('已审批')
  load()
}
onMounted(load)
</script>

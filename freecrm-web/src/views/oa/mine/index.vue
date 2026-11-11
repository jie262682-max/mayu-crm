<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="实例" width="70" />
      <el-table-column prop="bizTitle" label="申请" min-width="200" />
      <el-table-column label="业务" width="100">
        <template #default="{ row }">{{ ['合同', '回款', '请假'][row.bizType - 1] || row.bizType }}</template>
      </el-table-column>
      <el-table-column prop="currentLevel" label="当前节点" width="85" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['warning', 'success', 'danger', 'info'][row.status]" size="small">
            {{ ['进行中', '已通过', '已驳回', '已撤回'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发起时间" width="165" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link @click="showTasks(row)">轨迹</el-button>
          <el-button v-if="row.status === 0" link type="warning" @click="withdraw(row)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" title="审批轨迹" width="560px">
      <el-timeline>
        <el-timeline-item v-for="t in tasks" :key="t.id" :timestamp="t.auditTime || '待处理'" placement="top">
          节点{{ t.nodeLevel }} — 审批人{{ t.approverUserId }} —
          <b>{{ ['待办', '通过', '驳回'][t.status] }}</b>
          <span v-if="t.opinion" style="color:#999">（{{ t.opinion }}）</span>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const dlg = ref(false); const tasks = ref([])

async function load() {
  const p = await http.get('/api/oa/flow/mine', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records; total.value = Number(p.total)
}
async function showTasks(row) {
  tasks.value = await http.get('/api/oa/flow/tasks', { params: { instanceId: row.id } })
  dlg.value = true
}
async function withdraw(row) {
  await ElMessageBox.confirm('撤回该申请？', '提示', { type: 'warning' })
  await http.put(`/api/oa/flow/withdraw?instanceId=${row.id}`)
  ElMessage.success('已撤回'); load()
}
onMounted(load)
</script>

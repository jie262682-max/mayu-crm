<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-radio-group v-model="onlyMine" @change="load">
        <el-radio-button :value="false">全部</el-radio-button>
        <el-radio-button :value="true">我的</el-radio-button>
      </el-radio-group>
      <el-button type="primary" plain style="margin-left:8px" @click="dlg = true">发起请假</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userId" label="申请人ID" width="90" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ ['事假', '病假', '年假', '调休'][row.type - 1] }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始" width="160" />
      <el-table-column prop="endTime" label="结束" width="160" />
      <el-table-column prop="days" label="天数" width="70" />
      <el-table-column prop="reason" label="事由" min-width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['warning', 'success', 'danger'][row.auditStatus]" size="small">
            {{ ['审批中', '已通过', '已驳回'][row.auditStatus] }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" title="发起请假" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="(n, i) in ['事假', '病假', '年假', '调休']" :key="i" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="天数"><el-input-number v-model="form.days" :min="0.5" :step="0.5" style="width:100%" /></el-form-item>
        <el-form-item label="事由"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="apply">提交（自动进审批）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const onlyMine = ref(false)
const dlg = ref(false)
const form = reactive({ type: 1, startTime: '', endTime: '', days: 1, reason: '' })

async function load() {
  const p = await http.get('/api/oa/leave/page', {
    params: { pageNo: pageNo.value, pageSize: 10, mine: onlyMine.value || undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
async function apply() {
  await http.post('/api/oa/leave/apply', form)
  ElMessage.success('已提交审批'); dlg.value = false; onlyMine.value = true; load()
}
onMounted(load)
</script>

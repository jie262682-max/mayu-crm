<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-radio-group v-model="onlyMine" @change="load">
        <el-radio-button :value="false">全部申请</el-radio-button>
        <el-radio-button :value="true">我的</el-radio-button>
      </el-radio-group>
      <el-select v-model="formId" placeholder="表单" clearable style="width:160px" @change="load">
        <el-option v-for="d in forms" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-select v-model="statusF" placeholder="状态" clearable style="width:120px" @change="load">
        <el-option label="审批中" :value="0" /><el-option label="已通过" :value="1" /><el-option label="已驳回" :value="2" />
      </el-select>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="表单" width="110">
        <template #default="{ row }">{{ formName(row.formId) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column label="申请人" width="90">
        <template #default="{ row }">{{ userMap[row.userId] || row.userId }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['warning','success','danger'][row.auditStatus]" size="small">
            {{ ['审批中','已通过','已驳回'][row.auditStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="160" />
      <el-table-column label="详情" min-width="220">
        <template #default="{ row }">{{ dataText(row) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const onlyMine = ref(true); const formId = ref(null); const statusF = ref(null)
const forms = ref([]); const userMap = ref({})

function formName(id) {
  const d = forms.value.find(f => f.id === id)
  return d ? d.name : '#' + id
}
function dataText(row) {
  try {
    const data = JSON.parse(row.formData || '{}')
    const def = forms.value.find(f => f.id === row.formId)
    const fields = def ? JSON.parse(def.fields) : []
    return fields.filter(f => data[f.key] !== undefined && data[f.key] !== null)
      .map(f => `${f.label}：${data[f.key]}`).join('；').slice(0, 120)
  } catch { return '-' }
}
async function load() {
  const p = await http.get('/api/oa/form/apply/page', {
    params: { pageNo: pageNo.value, pageSize: 10, mine: onlyMine.value || undefined, formId: formId.value || undefined, auditStatus: statusF.value ?? undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
onMounted(async () => {
  forms.value = await http.get('/api/oa/form/def/enabled') || []
  const list = await http.get('/api/user/simple-list')
  const m = {}; (list || []).forEach(u => { m[u.id] = u.nickname })
  userMap.value = m
  load()
})
</script>

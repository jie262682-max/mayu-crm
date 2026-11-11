<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="商机名称" style="width:200px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建商机</el-button>
      <el-button @click="groupDlg = true">状态组管理</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商机名" min-width="140" />
      <el-table-column prop="customerId" label="客户ID" width="90" />
      <el-table-column label="当前阶段" width="120">
        <template #default="{ row }">{{ stageName(row.stageId) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column prop="expectedDealDate" label="预计成交" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.endStatus === 1" type="success">赢单</el-tag>
          <el-tag v-else-if="row.endStatus === 2" type="danger">输单</el-tag>
          <el-tag v-else type="info">进行中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-select v-if="!row.endStatus" :model-value="row.stageId" size="small" style="width:100px"
                     @change="v => moveStage(row, v)">
            <el-option v-for="s in stages(row.stageGroupId)" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-button v-if="!row.endStatus" link type="success" @click="end(row, 1)">赢单</el-button>
          <el-button v-if="!row.endStatus" link type="danger" @click="end(row, 2)">输单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑商机' : '新建商机'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商机名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="状态组">
          <el-select v-model="form.stageGroupId" style="width:100%" @change="loadStages">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="预计成交">
          <el-date-picker v-model="form.expectedDealDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupDlg" title="状态组管理" width="560px">
      <el-form :inline="true">
        <el-form-item label="组名"><el-input v-model="ng.name" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="addGroup">新建</el-button></el-form-item>
      </el-form>
      <el-table :data="groups" size="small" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="组名" />
        <el-table-column label="阶段" min-width="200">
          <template #default="{ row }">{{ stages(row.id).map(s => s.name).join(' → ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="delGroup(row)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 新组默认三阶段
const ng = reactive({ name: '' })
const groups = ref([])
const stageCache = reactive({})
const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')
const dlg = ref(false); const groupDlg = ref(false)
const form = reactive({})

function stages(gid) { return stageCache[gid] || [] }
function stageName(sid) {
  for (const list of Object.values(stageCache)) {
    const s = list.find(x => x.id === sid)
    if (s) return s.name
  }
  return sid ? '#' + sid : '-'
}

async function loadGroups() {
  groups.value = await http.get('/api/crm/opportunity/stage-group/list')
  for (const g of groups.value) {
    stageCache[g.id] = await http.get('/api/crm/opportunity/stage/list', { params: { groupId: g.id } })
  }
}
function loadStages(gid) {
  if (!stageCache[gid]) {
    http.get('/api/crm/opportunity/stage/list', { params: { groupId: gid } }).then(l => stageCache[gid] = l)
  }
}

async function load() {
  const p = await http.get('/api/crm/opportunity/page', { params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || { amount: 0 })
  if (form.stageGroupId) loadStages(form.stageGroupId)
  dlg.value = true
}
async function save() {
  await http.post('/api/crm/opportunity/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function moveStage(row, stageId) {
  await http.put(`/api/crm/opportunity/move-stage?id=${row.id}&stageId=${stageId}`)
  ElMessage.success('已推进'); load()
}
async function end(row, endStatus) {
  await ElMessageBox.confirm(`确认标记商机「${row.name}」为${endStatus === 1 ? '赢单（客户将标记成交）' : '输单'}？`, '提示', { type: 'warning' })
  await http.put(`/api/crm/opportunity/end?id=${row.id}&endStatus=${endStatus}`)
  ElMessage.success('已结束'); load()
}
async function addGroup() {
  if (!ng.name) return
  await http.post('/api/crm/opportunity/stage-group/save', {
    name: ng.name,
    stages: [
      { name: '初步接触', percent: 10 },
      { name: '方案报价', percent: 40 },
      { name: '商务谈判', percent: 70 },
      { name: '赢单', percent: 100 }
    ]
  })
  ElMessage.success('已创建（默认四阶段）'); ng.name = ''; loadGroups()
}
async function delGroup(row) {
  await ElMessageBox.confirm(`删除状态组「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/crm/opportunity/stage-group/delete', { params: { id: row.id } })
  loadGroups()
}
onMounted(() => { loadGroups(); load() })
</script>

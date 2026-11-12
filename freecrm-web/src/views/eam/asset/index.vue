<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px;flex-wrap:wrap">
      <el-input v-model="kw" placeholder="名称/编号" style="width:180px" clearable @keyup.enter="load" />
      <el-cascader v-model="catPath" :options="catTree" :props="{ value: 'id', label: 'name', checkStrictly: true }"
                   placeholder="分类" clearable style="width:170px" @change="load" />
      <el-select v-model="statusF" placeholder="状态" clearable style="width:110px" @change="load">
        <el-option v-for="(n, i) in ['闲置', '在用', '维修', '报废']" :key="i" :label="n" :value="i" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新增资产</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="assetNo" label="资产编号" width="150" />
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ catName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column prop="model" label="型号" min-width="100" />
      <el-table-column prop="purchaseAmount" label="购置金额" width="100" />
      <el-table-column prop="useYears" label="年限" width="60" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="['info', 'success', 'warning', 'danger'][row.status]" size="small">
            {{ ['闲置', '在用', '维修', '报废'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="保管人" width="100">
        <template #default="{ row }">{{ userMap[row.keeperUserId] || row.keeperUserId }}</template>
      </el-table-column>
      <el-table-column prop="location" label="位置" width="110" />
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="op('/receive', row, '领用')">领用</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="op('/give-back', row, '退回')">退回</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="op('/lend', row, '借出', true)">借出</el-button>
          <el-button v-if="row.status !== 2 && row.status !== 3" link @click="op('/repair', row, '报修')">报修</el-button>
          <el-button v-if="row.status === 2" link type="success" @click="op('/repair-done', row, '维修完成')">修完</el-button>
          <el-button v-if="row.status !== 3" link type="danger" @click="op('/scrap', row, '报废')">报废</el-button>
          <el-button link @click="showChanges(row)">历史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑资产' : '新增资产'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="资产编号"><el-input v-model="form.assetNo" placeholder="留空自动生成" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-cascader v-model="formCat" :options="catTree" :props="{ value: 'id', label: 'name', checkStrictly: true }" style="width:100%" />
        </el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="序列号"><el-input v-model="form.serialNo" /></el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="购置金额"><el-input-number v-model="form.purchaseAmount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="折旧年限"><el-input-number v-model="form.useYears" :min="1" :max="30" style="width:100%" /></el-form-item>
        <el-form-item label="使用部门ID"><el-input-number v-model="form.deptId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="存放位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const userMap = ref({})
async function loadUsers() {
  const list = await http.get('/api/user/simple-list')
  const m = {}
  ;(list || []).forEach(u => { m[u.id] = u.nickname })
  userMap.value = m
}
const kw = ref(''); const statusF = ref(null); const catPath = ref([])
const catTree = ref([]); const catFlat = ref([])
const dlg = ref(false); const form = reactive({})
const formCat = ref([])

async function load() {
  const catId = catPath.value.length ? catPath.value[catPath.value.length - 1] : undefined
  const p = await http.get('/api/eam/asset/page', {
    params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined, categoryId: catId, status: statusF.value ?? undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
async function loadCats() {
  catTree.value = await http.get('/api/eam/category/tree')
  const flat = []
  const walk = list => list.forEach(c => { flat.push(c); walk(c.children || []) })
  walk(catTree.value)
  catFlat.value = flat
}
function catName(id) {
  return catFlat.value.find(c => c.id === id)?.name || '-'
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || { useYears: 5, purchaseAmount: 0 })
  formCat.value = row?.categoryId ? [row.categoryId] : []
  dlg.value = true
}
async function save() {
  if (formCat.value.length) {
    form.categoryId = formCat.value[formCat.value.length - 1]
  }
  await http.post('/api/eam/asset/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function op(path, row, label, needUser) {
  let toUser
  if (path === '/receive' || path === '/lend') {
    const v = await ElMessageBox.prompt(
      `${label}「${row.name}」${path === '/receive' ? '（留空=自己领用）' : '（输入借用人ID）'}\n理由可写在ID后`, label, {})
      .catch(() => null)
    if (v === null) return
    const [uid, ...reason] = String(v.value || '').trim().split(/\s+/)
    toUser = uid && !isNaN(Number(uid)) ? Number(uid) : null
    var reasonStr = reason.join(' ')
    await http.put('/api/eam/asset' + path, null, {
      params: { id: row.id, toUserId: toUser || undefined, reason: reasonStr || undefined }
    })
  } else {
    const reason = await ElMessageBox.prompt(`${label}「${row.name}」的原因：`, label, {}).catch(() => null)
    if (reason === null) return
    const r = await http.put('/api/eam/asset' + path, null, { params: { id: row.id, reason: reason.value || undefined } })
    if (path === '/scrap' && r?.flowStarted) {
      ElMessage.success('已提交报废审批')
    }
  }
  ElMessage.success(label + '完成')
  load()
}
async function showChanges(row) {
  const p = await http.get('/api/eam/asset/change/page', { params: { assetId: row.id, pageSize: 50 } })
  const names = ['', '领用', '退回', '借出', '归还', '维修', '报废']
  ElMessageBox.alert(
    p.records.map(c => `[${c.changeTime}] ${names[c.type]}：${c.fromUserId || '-'} → ${c.toUserId || '-'} ${c.reason || ''}`).join('<br>') || '无记录',
    `变更历史 - ${row.assetNo}`, { dangerouslyUseHTMLString: true })
}
onMounted(() => { loadUsers(); loadCats(); load() })
</script>

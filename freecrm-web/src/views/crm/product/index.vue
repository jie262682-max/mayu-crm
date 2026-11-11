<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="名称/编码" style="width:200px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建产品</el-button>
      <el-button @click="catDlg = true">分类管理</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="no" label="编码" width="120" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ catName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column label="单位" width="80">
        <template #default="{ row }">{{ ['件', '套', '年', '次'][row.unit - 1] || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑产品' : '新建产品'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.no" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" clearable style="width:100%">
            <el-option v-for="c in cats" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.unit" style="width:100%">
            <el-option v-for="(n, i) in ['件', '套', '年', '次']" :key="i" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="catDlg" title="分类管理" width="420px">
      <el-form :inline="true">
        <el-form-item><el-input v-model="nc.name" placeholder="分类名" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" size="small" @click="addCat">新增</el-button></el-form-item>
      </el-form>
      <el-table :data="cats" size="small" border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="delCat(row)">删</el-button>
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

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')
const dlg = ref(false); const form = reactive({})
const catDlg = ref(false); const cats = ref([]); const nc = reactive({ name: '' })

const catName = id => (cats.value.find(c => c.id === id) || {}).name || '-'

async function loadCats() {
  cats.value = await http.get('/api/crm/product/category/list')
}
async function load() {
  const p = await http.get('/api/crm/product/page', { params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || { status: 1, unit: 1 })
  dlg.value = true
}
async function save() {
  await http.post('/api/crm/product/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function del(row) {
  await ElMessageBox.confirm(`删除产品「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/crm/product/delete', { params: { id: row.id } })
  load()
}
async function addCat() {
  if (!nc.name) return
  await http.post('/api/crm/product/category/save', { name: nc.name })
  nc.name = ''; loadCats()
}
async function delCat(row) {
  await http.delete('/api/crm/product/category/delete', { params: { id: row.id } })
  loadCats()
}
onMounted(() => { loadCats(); load() })
</script>

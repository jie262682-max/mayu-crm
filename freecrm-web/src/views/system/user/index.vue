<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="用户名/昵称" style="width:220px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建用户</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="mobile" label="手机" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新建用户'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" placeholder="留空则不修改" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
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

const rows = ref([])
const total = ref(0)
const pageNo = ref(1)
const kw = ref('')
const dlg = ref(false)
const form = reactive({ id: null, username: '', nickname: '', password: '', mobile: '', email: '', status: 1 })

async function load() {
  const p = await http.get('/api/user/page', { params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined } })
  rows.value = p.records
  total.value = Number(p.total)
}
function openForm(row) {
  Object.assign(form, row || { id: null, username: '', nickname: '', password: '', mobile: '', email: '', status: 1 })
  dlg.value = true
}
async function save() {
  await http.post('/api/user/save', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.nickname}」？`, '提示', { type: 'warning' })
  await http.delete('/api/user/delete', { params: { id: row.id } })
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

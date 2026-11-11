<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-button type="primary" @click="openForm(null, 0)">新建顶级部门</el-button>
    </div>
    <el-table :data="tree" border row-key="id" default-expand-all
              :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="部门名称" min-width="220" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(null, row.id)">加子部门</el-button>
          <el-button link type="primary" @click="openForm(row, row.parentId)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑部门' : '新建部门'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="负责人ID"><el-input v-model.number="form.leaderUserId" placeholder="用户 ID" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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

const tree = ref([])
const dlg = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', leaderUserId: null, sort: 0, status: 1 })

async function load() {
  tree.value = await http.get('/api/dept/tree')
}
function openForm(row, parentId) {
  Object.assign(form, row || { id: null, parentId, name: '', leaderUserId: null, sort: 0, status: 1 })
  dlg.value = true
}
async function save() {
  await http.post('/api/dept/save', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`确认删除部门「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/dept/delete', { params: { id: row.id } })
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

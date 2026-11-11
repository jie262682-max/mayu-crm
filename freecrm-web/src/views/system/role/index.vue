<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-button type="primary" @click="openForm()">新建角色</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="角色名称" width="160" />
      <el-table-column prop="code" label="角色编码" width="160" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="warning" @click="openAssign(row)">菜单授权</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新建角色'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="form.code" placeholder="如 sales_leader" :disabled="form.code === 'super_admin'" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDlg" :title="`菜单授权：${cur.name}`" width="440px">
      <el-tree ref="treeEl" :data="menuTree" node-key="id" show-checkbox default-expand-all
               :props="{ label: 'name', children: 'children' }" />
      <template #footer>
        <el-button @click="assignDlg = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存授权</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const dlg = ref(false)
const form = reactive({ id: null, name: '', code: '', sort: 0, status: 1, remark: '' })
const assignDlg = ref(false)
const cur = ref({})
const menuTree = ref([])
const treeEl = ref()

async function load() {
  rows.value = await http.get('/api/role/list')
}
function openForm(row) {
  Object.assign(form, row || { id: null, name: '', code: '', sort: 0, status: 1, remark: '' })
  dlg.value = true
}
async function save() {
  await http.post('/api/role/save', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/role/delete', { params: { id: row.id } })
  ElMessage.success('已删除')
  load()
}
async function openAssign(row) {
  cur.value = row
  menuTree.value = await http.get('/api/menu/tree')
  const ids = await http.get('/api/role/menu-ids', { params: { roleId: row.id } })
  assignDlg.value = true
  // 勾选叶子节点（父级自动联动，避免父勾全选子）
  setTimeout(() => {
    const leaf = new Set()
    const walk = list => list.forEach(m => {
      if (m.children && m.children.length) walk(m.children)
      else leaf.add(m.id)
    })
    walk(menuTree.value)
    treeEl.value.setCheckedKeys(ids.filter(id => leaf.has(id)))
  })
}
async function saveAssign() {
  const checked = treeEl.value.getCheckedKeys()
  const half = treeEl.value.getHalfCheckedKeys()
  await http.post('/api/role/assign-menus', [...checked, ...half], { params: { roleId: cur.value.id } })
  ElMessage.success('授权已保存（该角色用户重新登录生效）')
  assignDlg.value = false
}
onMounted(load)
</script>

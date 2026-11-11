<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-button type="primary" @click="openForm(null, 0, 0)">新建顶级目录</el-button>
    </div>
    <el-table :data="tree" border row-key="id" default-expand-all
              :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="['', 'info', 'success', 'warning'][row.type]" size="small">
            {{ ['', '目录', '页面', '按钮'][row.type] || row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permission" label="权限码" min-width="150" />
      <el-table-column prop="path" label="路径" min-width="110" />
      <el-table-column prop="component" label="组件" min-width="150" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="可见" width="70">
        <template #default="{ row }">
          <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">{{ row.visible === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button v-if="row.type !== 3" link type="primary" @click="openForm(null, row.id, row.type)">加子项</el-button>
          <el-button link type="primary" @click="openForm(row, row.parentId, row.type)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑菜单' : '新建菜单'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button :value="1">目录</el-radio-button>
            <el-radio-button :value="2">页面</el-radio-button>
            <el-radio-button :value="3">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="权限码"><el-input v-model="form.permission" placeholder="如 crm:lead:save，目录可空" /></el-form-item>
        <template v-if="form.type !== 3">
          <el-form-item label="路由路径"><el-input v-model="form.path" placeholder="目录如 /crm，页面如 lead" /></el-form-item>
          <el-form-item label="组件"><el-input v-model="form.component" placeholder="目录填 Layout，页面如 crm/lead/index" /></el-form-item>
          <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Element Plus 图标名" /></el-form-item>
        </template>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item v-if="form.type !== 3" label="侧栏可见">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
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

const tree = ref([])
const dlg = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', type: 2, path: '', component: '', permission: '', icon: '', sort: 0, visible: 1 })

async function load() {
  tree.value = await http.get('/api/menu/tree')
}
function openForm(row, parentId, parentType) {
  if (row) {
    Object.assign(form, { ...row, component: row.component || '', permission: row.permission || '', icon: row.icon || '' })
  } else {
    const type = parentType === 1 ? 2 : 1
    Object.assign(form, { id: null, parentId, name: '', type, path: '', component: '', permission: '', icon: '', sort: 0, visible: 1 })
    if (type === 1) form.component = 'Layout'
  }
  dlg.value = true
}
async function save() {
  await http.post('/api/menu/save', form)
  ElMessage.success('保存成功（角色需重新授权/用户重新登录生效）')
  dlg.value = false
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」？子菜单/授权将一并失效`, '提示', { type: 'warning' })
  await http.delete('/api/menu/delete', { params: { id: row.id } })
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-button type="primary" plain @click="dlg = true; form = { decisionRole: 3 }">新增联系人</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="customerId" label="客户ID" width="90" />
      <el-table-column prop="mobile" label="手机" width="130" />
      <el-table-column prop="post" label="职务" width="120" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">{{ ['决策者', '影响者', '使用者', '教练'][row.decisionRole - 1] || '-' }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" title="新增联系人" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="职务"><el-input v-model="form.post" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.decisionRole" style="width:100%">
            <el-option v-for="(n, i) in ['决策者', '影响者', '使用者', '教练']" :key="n" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const dlg = ref(false); const form = ref({ decisionRole: 3 })

async function load() {
  const p = await http.get('/api/crm/contact/page', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records; total.value = Number(p.total)
}
async function save() {
  await http.post('/api/crm/contact/save', form.value)
  ElMessage.success('已保存'); dlg.value = false; load()
}
onMounted(load)
</script>

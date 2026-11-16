<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-button type="primary" @click="openDlg">申请开票</el-button>
      <span style="color:#999;font-size:13px;align-self:center">已审合同可申请；专票需税号；开票累计不得超过合同额</span>
    </div>
    <el-table :data="rows" border size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="合同" min-width="150">
        <template #default="{ row }">{{ contractNo(row.contractId) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额（元）" width="120" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.type === 2 ? '专票' : '普票' }}</template>
      </el-table-column>
      <el-table-column prop="title" label="票头" min-width="160" show-overflow-tooltip />
      <el-table-column prop="taxNo" label="税号" width="170" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'" size="small">
            {{ ['待审', '已开', '驳回'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" title="申请开票" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="合同">
          <el-select v-model="form.contractId" filterable style="width:100%" @change="onContract">
            <el-option v-for="c in contracts" :key="c.id" :value="c.id"
                       :label="c.contractNo + '（' + c.name + '）'" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.contractId" label="已开/待审">
          <span style="font-size:13px;color:#666">已开 {{ sum.invoiced ?? 0 }} 元；待审 {{ sum.pending ?? 0 }} 元；合同额 {{ curTotal }} 元</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">普票</el-radio>
            <el-radio :value="2">专票</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额（元）"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width:200px" /></el-form-item>
        <el-form-item label="票头"><el-input v-model="form.title" placeholder="单位名称" /></el-form-item>
        <el-form-item v-if="form.type === 2" label="税号"><el-input v-model="form.taxNo" placeholder="纳税人识别号（专票必填）" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="apply">提交申请</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const contracts = ref([]); const dlg = ref(false); const sum = ref({})
const form = ref({ contractId: null, type: 1, amount: null, title: '', taxNo: '', remark: '' })

onMounted(async () => {
  const p = await http.get('/api/crm/contract/page', { params: { pageNo: 1, pageSize: 200 } })
  contracts.value = (p.records || []).filter(c => c.auditStatus === 2)
  load()
})

async function load() {
  const p = await http.get('/api/crm/invoice/page', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records || []; total.value = p.total || 0
}

const contractNo = id => { const c = contracts.value.find(x => x.id === id); return c ? c.contractNo : ('合同' + id) }
const curTotal = () => { const c = contracts.value.find(x => x.id === form.value.contractId); return c ? c.totalAmount : '-' }

async function onContract() {
  sum.value = await http.get('/api/crm/invoice/sum', { params: { contractId: form.value.contractId } })
}

function openDlg() {
  form.value = { contractId: null, type: 1, amount: null, title: '', taxNo: '', remark: '' }
  sum.value = {}
  dlg.value = true
}

async function apply() {
  await http.post('/api/crm/invoice/apply', form.value)
  ElMessage.success('已提交，等待审批')
  dlg.value = false
  load()
}
</script>

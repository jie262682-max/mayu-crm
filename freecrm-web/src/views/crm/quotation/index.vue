<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-button type="primary" @click="openForm()">新建报价</el-button>
      <span style="color:#999;font-size:13px;align-self:center">草稿→确认→转合同；转合同自动带出金额与明细</span>
    </div>
    <el-table :data="rows" border size="small">
      <el-table-column prop="quoteNo" label="报价编号" width="160" />
      <el-table-column prop="customerName" label="客户" min-width="140" />
      <el-table-column prop="totalAmount" label="金额（元）" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['info', 'success', 'primary', 'danger'][row.status]" size="small">
            {{ ['草稿', '已确认', '已转合同', '已作废'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button link @click="show(row)">查看/打印</el-button>
          <el-button v-if="row.status === 0" link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="confirm(row)">确认</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="toContract(row)">转合同</el-button>
          <el-button v-if="row.status <= 1" link type="danger" @click="voidQ(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑报价' : '新建报价'" width="760px">
      <el-form :model="form" label-width="70px" inline>
        <el-form-item label="客户">
          <el-select v-model="form.customerId" filterable style="width:240px">
            <el-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="商机">
          <el-select v-model="form.opportunityId" clearable style="width:240px">
            <el-option v-for="o in opps" :key="o.id" :value="o.id" :label="o.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="折让">
          <el-input-number v-model="form.discount" :min="0" :precision="2" :controls="false" style="width:120px" />
        </el-form-item>
      </el-form>
      <el-table :data="items" border size="small">
        <el-table-column label="产品" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.productId" filterable style="width:100%" @change="pid => onProduct(row, pid)">
              <el-option v-for="p in products" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :controls="false" style="width:110px" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0.01" :precision="2" :controls="false" style="width:90px" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="110">
          <template #default="{ row }">{{ ((row.price || 0) * (row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column width="60">
          <template #default="{ $index }">
            <el-button link type="danger" @click="items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" style="margin-top:8px" @click="addItem">+ 加一行</el-button>
      <div style="margin-top:8px;text-align:right;font-size:14px">
        合计 {{ itemsSum }} − 折让 {{ form.discount || 0 }} =
        <b style="color:#409EFF">{{ (itemsSum - (form.discount || 0)).toFixed(2) }}</b> 元
      </div>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="printDlg" title="报价单" width="720px">
      <div id="print-area">
        <h2 style="text-align:center;margin:0 0 4px">报 价 单</h2>
        <div style="text-align:center;color:#666;margin-bottom:12px">{{ cur.quoteNo }} · {{ cur.createTime }}</div>
        <p>客户：<b>{{ cur.customerName }}</b></p>
        <table border="1" cellspacing="0" cellpadding="6" style="width:100%;border-collapse:collapse;font-size:13px">
          <tr style="background:#f5f7fa"><th>产品</th><th>单价</th><th>数量</th><th>小计</th></tr>
          <tr v-for="i in cur.items || []" :key="i.id">
            <td>{{ i.name }}</td><td>{{ i.price }}</td><td>{{ i.quantity }}</td><td>{{ i.amount }}</td>
          </tr>
          <tr>
            <td colspan="3" style="text-align:right">折让</td><td>{{ cur.discount }}</td>
          </tr>
          <tr>
            <td colspan="3" style="text-align:right"><b>合计（元）</b></td><td><b>{{ cur.totalAmount }}</b></td>
          </tr>
        </table>
        <p v-if="cur.remark">备注：{{ cur.remark }}</p>
        <p style="display:flex;justify-content:space-between;margin-top:24px">
          <span>报价人：{{ userName(cur.ownerUserId) }}</span><span>客户签字：＿＿＿＿＿＿</span>
        </p>
      </div>
      <template #footer>
        <el-button @click="printDlg = false">关闭</el-button>
        <el-button type="primary" @click="doPrint">打印</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const dlg = ref(false); const printDlg = ref(false)
const customers = ref([]); const opps = ref([]); const products = ref([]); const users = ref([])
const items = ref([])
const form = ref({ customerId: null, opportunityId: null, discount: 0, remark: '' })
const cur = ref({})

onMounted(async () => {
  const [cs, os, ps, us] = await Promise.all([
    http.get('/api/crm/customer/page', { params: { pageNo: 1, pageSize: 500 } }),
    http.get('/api/crm/opportunity/page', { params: { pageNo: 1, pageSize: 500 } }).catch(() => ({})),
    http.get('/api/crm/product/simple-list').catch(() => []),
    http.get('/api/user/simple-list').catch(() => [])
  ])
  customers.value = cs.records || []
  opps.value = os.records || []
  products.value = ps || []
  users.value = us || []
  load()
})

async function load() {
  const p = await http.get('/api/crm/quotation/page', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records || []; total.value = p.total || 0
}

const itemsSum = () => items.value.reduce((s, i) => s + (i.price || 0) * (i.quantity || 0), 0)
const userName = id => { const u = users.value.find(x => x.id === id); return u ? u.nickname : ('用户' + id) }

function addItem() { items.value.push({ productId: null, name: '', price: 0, quantity: 1 }) }
function onProduct(row, pid) {
  const p = products.value.find(x => x.id === pid)
  if (p) { row.name = p.name; row.price = p.price }
}

async function openForm(row) {
  if (row) {
    const d = await http.get('/api/crm/quotation/get', { params: { id: row.id } })
    form.value = { id: d.id, customerId: d.customerId, opportunityId: d.opportunityId, discount: d.discount, remark: d.remark }
    items.value = (d.items || []).map(i => ({ productId: i.productId, name: i.name, price: i.price, quantity: i.quantity }))
  } else {
    form.value = { customerId: null, opportunityId: null, discount: 0, remark: '' }
    items.value = []
  }
  dlg.value = true
}

async function save() {
  if (!form.value.customerId) return ElMessage.warning('选客户')
  if (!items.value.length) return ElMessage.warning('至少一行明细')
  await http.post('/api/crm/quotation/save', { quotation: form.value, items: items.value })
  ElMessage.success('已保存'); dlg.value = false; load()
}

async function confirm(row) {
  await ElMessageBox.confirm(`确认报价 ${row.quoteNo}？确认后不可编辑`, '确认', { type: 'warning' })
  await http.put(`/api/crm/quotation/confirm?id=${row.id}`)
  ElMessage.success('已确认'); load()
}

async function toContract(row) {
  await ElMessageBox.confirm(`由报价 ${row.quoteNo} 生成合同草稿（金额 ${row.totalAmount} 元，明细自动带出）？`, '转合同', { type: 'warning' })
  const cid = await http.put(`/api/crm/quotation/to-contract?id=${row.id}`)
  ElMessage.success(`已生成合同 ${cid}（草稿，待提交审批）`)
  load()
}

async function voidQ(row) {
  await ElMessageBox.confirm(`作废报价 ${row.quoteNo}？`, '作废', { type: 'warning' })
  await http.put(`/api/crm/quotation/void?id=${row.id}`)
  ElMessage.success('已作废'); load()
}

async function show(row) {
  cur.value = await http.get('/api/crm/quotation/get', { params: { id: row.id } })
  printDlg.value = true
}

function doPrint() {
  const html = document.getElementById('print-area').innerHTML
  const w = window.open('', '_blank')
  w.document.write(`<html><head><title>报价单</title><style>
    body{font-family:"Microsoft YaHei";padding:24px} table{width:100%;border-collapse:collapse}
    th,td{border:1px solid #333;padding:6px;font-size:13px} th{background:#f5f7fa}
  </style></head><body>${html}</body></html>`)
  w.document.close(); w.print()
}
</script>

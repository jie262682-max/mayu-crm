<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="合同名/编号" style="width:200px" clearable @keyup.enter="load" />
      <el-select v-model="auditFilter" placeholder="审核状态" clearable style="width:130px">
        <el-option label="草稿" :value="0" /><el-option label="审批中" :value="1" />
        <el-option label="已通过" :value="2" /><el-option label="已驳回" :value="3" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建合同</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="contractNo" label="合同编号" width="160" />
      <el-table-column prop="name" label="合同名" min-width="130" />
      <el-table-column prop="customerId" label="客户ID" width="85" />
      <el-table-column prop="orderDate" label="下单日期" width="105" />
      <el-table-column prop="totalAmount" label="总额" width="100" />
      <el-table-column prop="receivedAmount" label="已回款" width="100" />
      <el-table-column label="审核" width="90">
        <template #default="{ row }">
          <el-tag :type="['info', 'warning', 'success', 'danger'][row.auditStatus]" size="small">
            {{ ['草稿', '审批中', '已通过', '已驳回'][row.auditStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 0" link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-if="row.auditStatus === 0" link type="warning" @click="submit(row)">提审</el-button>
          <el-button v-if="row.auditStatus === 1" link type="success" @click="audit(row, true)">通过</el-button>
          <el-button v-if="row.auditStatus === 1" link type="danger" @click="audit(row, false)">驳回</el-button>
          <el-button v-if="row.auditStatus === 2" link type="success" @click="openPay(row)">回款</el-button>
          <el-button link @click="openPlans(row)">计划</el-button>
          <el-button link type="info" @click="openItems(row)">明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑合同' : '新建合同'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="合同名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="商机ID"><el-input-number v-model="form.opportunityId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="下单日期">
          <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="总金额"><el-input-number v-model="form.totalAmount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="planDlg" title="回款计划" width="560px">
      <el-form :inline="true">
        <el-form-item><el-input-number v-model="np.periodNo" :min="1" placeholder="期数" style="width:90px" /></el-form-item>
        <el-form-item>
          <el-date-picker v-model="np.returnTime" type="date" value-format="YYYY-MM-DD" placeholder="回款日" />
        </el-form-item>
        <el-form-item><el-input-number v-model="np.amount" :min="0" placeholder="金额" style="width:120px" /></el-form-item>
        <el-form-item><el-button type="primary" size="small" @click="savePlan">加计划</el-button></el-form-item>
      </el-form>
      <el-table :data="plans" size="small" border>
        <el-table-column prop="periodNo" label="期" width="60" />
        <el-table-column prop="returnTime" label="回款日" width="110" />
        <el-table-column prop="amount" label="金额" width="110" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="itemDlg" :title="`产品明细 - ${curContract?.contractNo || ''}`" width="760px">
      <template v-if="itemsEditable">
        <div style="display:flex;gap:8px;margin-bottom:8px">
          <el-select v-model="newItem.productId" filterable placeholder="选择产品" style="width:260px">
            <el-option v-for="p in products" :key="p.id" :label="p.name + '（￥' + p.price + '/' + p.unitName + '）'" :value="p.id" />
          </el-select>
          <el-input-number v-model="newItem.quantity" :min="0.01" :precision="2" placeholder="数量" style="width:120px" />
          <el-input-number v-model="newItem.price" :min="0" placeholder="单价" style="width:130px" />
          <el-button type="primary" size="small" @click="addItem">添加</el-button>
        </div>
      </template>
      <el-table :data="items" border size="small">
        <el-table-column prop="productName" label="产品" min-width="170" />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="price" label="单价" width="100" />
        <el-table-column prop="amount" label="小计" width="110" />
        <el-table-column prop="remark" label="备注" min-width="100" />
        <el-table-column v-if="itemsEditable" label="操作" width="60">
          <template #default="{ row, $index }">
            <el-button link type="danger" @click="items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:8px;text-align:right;font-weight:bold" v-if="items.length">
        明细合计：￥{{ itemsTotal }}
      </div>
      <template #footer>
        <el-button @click="itemDlg = false">关闭</el-button>
        <el-button v-if="itemsEditable" type="primary" @click="saveItems">保存明细</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDlg" :title="`录入回款 - ${curContract?.contractNo || ''}`" width="440px">
      <el-form :model="npay" label-width="80px">
        <el-form-item label="回款日期">
          <el-date-picker v-model="npay.returnTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="npay.amount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="方式">
          <el-select v-model="npay.method" style="width:100%">
            <el-option v-for="(n, i) in ['对公转账', '现金', '微信', '支付宝', '其他']" :key="i" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="npay.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDlg = false">取消</el-button>
        <el-button type="primary" @click="savePay">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref(''); const auditFilter = ref(null)
const dlg = ref(false); const form = reactive({})
const planDlg = ref(false); const plans = ref([]); const np = reactive({ periodNo: 1, returnTime: '', amount: 0 })
const payDlg = ref(false); const npay = reactive({ returnTime: '', amount: 0, method: 1, remark: '' })
const curContract = ref(null)
const itemDlg = ref(false); const items = ref([]); const itemsEditable = ref(false)
const products = ref([]); const newItem = reactive({ productId: null, quantity: 1, price: 0 })

async function openItems(row) {
  curContract.value = row
  itemsEditable.value = row.auditStatus === 0
  if (!products.value.length) {
    const p = await http.get('/api/crm/product/page', { params: { pageNo: 1, pageSize: 100 } })
    products.value = (p.records || []).map(x => ({ ...x, unitName: ['件', '套', '年', '次'][x.unit - 1] || '' }))
  }
  items.value = await http.get('/api/crm/contract/item/list', { params: { contractId: row.id } })
  items.value = items.value.map(it => {
    const prod = products.value.find(p => p.id === it.productId)
    return { ...it, productName: prod ? prod.name : ('产品#' + it.productId) }
  })
  newItem.productId = null; newItem.quantity = 1; newItem.price = 0
  itemDlg.value = true
}
function addItem() {
  const prod = products.value.find(p => p.id === newItem.productId)
  if (!prod) return
  const price = newItem.price || prod.price
  items.value.push({
    productId: prod.id, productName: prod.name,
    quantity: newItem.quantity, price,
    amount: +(Number(newItem.quantity) * Number(price)).toFixed(2)
  })
  newItem.productId = null; newItem.quantity = 1; newItem.price = 0
}
async function saveItems() {
  await http.post('/api/crm/contract/item/save-batch?contractId=' + curContract.value.id,
    items.value.map(({ productName, ...it }) => it))
  ElMessage.success('明细已保存')
  itemDlg.value = false
  load()
}
const itemsTotal = computed(() => items.value.reduce((s2, i) => s2 + Number(i.amount || 0), 0).toFixed(2))

async function load() {
  const p = await http.get('/api/crm/contract/page', {
    params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined, auditStatus: auditFilter.value ?? undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || {})
  dlg.value = true
}
async function save() {
  await http.post('/api/crm/contract/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function submit(row) {
  await ElMessageBox.confirm(`提交合同「${row.contractNo}」审批？`, '提示')
  await http.put(`/api/crm/contract/submit?id=${row.id}`)
  ElMessage.success('已提交'); load()
}
async function audit(row, pass) {
  await http.put(`/api/crm/contract/audit?id=${row.id}&pass=${pass}`)
  ElMessage.success(pass ? '已通过' : '已驳回'); load()
}
async function openPlans(row) {
  curContract.value = row
  plans.value = await http.get('/api/crm/contract/plan/list', { params: { contractId: row.id } })
  np.contractId = row.id
  planDlg.value = true
}
async function savePlan() {
  await http.post('/api/crm/contract/plan/save', np)
  ElMessage.success('已加计划')
  openPlans(curContract.value)
}
function openPay(row) {
  curContract.value = row
  npay.contractId = row.id
  npay.amount = null; npay.remark = ''
  payDlg.value = true
}
async function savePay() {
  if (!npay.amount) return ElMessage.warning('填写金额')
  await http.post('/api/crm/contract/payment/save', npay)
  ElMessage.success('回款已录入'); payDlg.value = false; load()
}
onMounted(load)
</script>

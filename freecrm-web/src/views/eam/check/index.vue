<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-button type="primary" @click="createDlg = true">创建盘点单</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="checkNo" label="盘点单号" width="150" />
      <el-table-column label="范围" width="120">
        <template #default="{ row }">{{ row.scopeType === 1 ? '全部资产' : '部门 #' + row.deptId }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'warning' : 'success'" size="small">{{ row.status === 0 ? '进行中' : '已完成' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalCount" label="资产数" width="80" />
      <el-table-column prop="diffCount" label="差异数" width="80">
        <template #default="{ row }"><span :style="{ color: row.diffCount > 0 ? '#f56c6c' : '' }">{{ row.diffCount }}</span></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openItems(row, false)">明细</el-button>
          <el-button link type="warning" @click="openItems(row, true)">仅差异</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="finish(row)">完成盘点</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="createDlg" title="创建盘点单" width="460px">
      <el-form label-width="90px">
        <el-form-item label="范围">
          <el-radio-group v-model="scope">
            <el-radio :value="1">全部资产</el-radio>
            <el-radio :value="2">按部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="scope === 2" label="部门ID">
          <el-input-number v-model="deptId" :min="1" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDlg" :title="`盘点明细 - ${cur?.checkNo || ''}`" width="860px">
      <el-table :data="items" border size="small">
        <el-table-column prop="assetId" label="资产ID" width="70" />
        <el-table-column prop="assetName" label="资产" min-width="140" />
        <el-table-column label="账面" width="70">
          <template #default="{ row }">{{ ['闲置', '在用', '维修', '报废'][row.expectStatus] }}</template>
        </el-table-column>
        <el-table-column label="实盘" width="150">
          <template #default="{ row }">
            <el-select v-if="cur?.status === 0" v-model="drafts[row.id]" size="small" placeholder="实盘状态" style="width:130px">
              <el-option v-for="(n, i) in ['闲置', '在用', '维修', '盘亏']" :key="i" :label="n" :value="i" />
            </el-select>
            <span v-else>{{ row.checkedTime ? (row.actualStatus === 3 ? '盘亏' : ['闲置', '在用', '维修'][row.actualStatus]) : '未盘' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.diff === 1" type="danger" size="small">差异</el-tag>
            <el-tag v-else-if="row.checkedTime" type="success" size="small">一致</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="cur?.status === 0" link type="primary" size="small"
                       @click="submitItem(row)">提交实盘</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([]); const total = ref(0); const pageNo = ref(1)
const createDlg = ref(false); const scope = ref(1); const deptId = ref(null); const remark = ref('')
const itemDlg = ref(false); const items = ref([]); const cur = ref(null); const drafts = ref({})
const assetNames = ref({})

async function load() {
  const p = await http.get('/api/eam/check/page', { params: { pageNo: pageNo.value, pageSize: 10 } })
  rows.value = p.records; total.value = Number(p.total)
}
async function create() {
  await http.post('/api/eam/check/create', { scopeType: scope.value, deptId: scope.value === 2 ? deptId.value : null, remark: remark.value })
  ElMessage.success('盘点单已创建'); createDlg.value = false; load()
}
async function openItems(row, onlyDiff) {
  cur.value = row
  items.value = await http.get('/api/eam/check/items', { params: { checkId: row.id, onlyDiff: onlyDiff || undefined } })
  // 补资产名
  const ap = await http.get('/api/eam/asset/page', { params: { pageNo: 1, pageSize: 1000 } })
  assetNames.value = {}
  ap.records.forEach(a => { assetNames.value[a.id] = a.assetNo + ' ' + a.name })
  items.value.forEach(i => { i.assetName = assetNames.value[i.assetId] || ('#' + i.assetId) })
  drafts.value = {}
  itemDlg.value = true
}
async function submitItem(row) {
  const v = drafts.value[row.id]
  if (v === undefined) return ElMessage.warning('先选实盘状态')
  const missing = v === 3
  await http.put('/api/eam/check/item', null, {
    params: { itemId: row.id, actualStatus: missing ? undefined : v, missing }
  })
  ElMessage.success('已记录')
  openItems(cur.value, false)
}
async function finish(row) {
  await ElMessageBox.confirm('完成盘点？未盘项不再记录。', '提示', { type: 'warning' })
  await http.put('/api/eam/check/finish', null, { params: { checkId: row.id } })
  ElMessage.success('已完成'); load()
}
onMounted(load)
</script>

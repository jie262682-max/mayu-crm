<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-button type="primary" plain @click="openForm()">新建流程</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="流程名" width="140" />
      <el-table-column label="业务" width="100">
        <template #default="{ row }">{{ ['合同审批', '回款审批', '请假'][row.bizType - 1] || row.bizType }}</template>
      </el-table-column>
      <el-table-column label="节点" min-width="280">
        <template #default="{ row }">
          <template v-for="(n, i) in parseNodes(row.nodes)" :key="i">
            <el-tag v-if="n.type === 'CONDITION'" type="warning" size="small" style="margin:2px 4px 2px 0">
              L{{ n.level }} 条件: {{ branchesText(n) }}
            </el-tag>
            <el-tag v-else :type="n.signType === 'ALL' ? 'danger' : ''" size="small" style="margin:2px 4px 2px 0">
              L{{ n.level }} {{ n.name || '审批' }}{{ n.signType === 'ALL' ? '(会签)' : '' }}: {{ (n.approverUserIds || []).join('/') }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑流程' : '新建流程'" width="680px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="流程名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="业务">
          <el-select v-model="form.bizType" style="width:100%">
            <el-option label="合同审批" :value="1" /><el-option label="回款审批" :value="2" /><el-option label="请假" :value="3" />
          </el-select>
          <div class="hint">可用条件变量：合同/回款 amount（金额）、contractAmount（合同总额）、请假 days（天数）</div>
        </el-form-item>
        <el-form-item label="节点">
          <div v-for="(n, i) in editNodes" :key="i" class="node-box">
            <div class="node-row">
              <span class="lv">L{{ i + 1 }}</span>
              <el-radio-group v-model="n.type" size="small">
                <el-radio-button value="APPROVAL">审批</el-radio-button>
                <el-radio-button value="CONDITION">条件</el-radio-button>
              </el-radio-group>
              <el-button size="small" type="danger" plain style="margin-left:auto" @click="editNodes.splice(i, 1)">删</el-button>
            </div>
            <template v-if="n.type === 'APPROVAL'">
              <div class="node-row">
                <el-input v-model="n.name" placeholder="节点名称（如 销售经理）" size="small" style="width:180px" />
                <el-radio-group v-model="n.signType" size="small">
                  <el-radio-button value="ANY">或签(任一)</el-radio-button>
                  <el-radio-button value="ALL">会签(全部)</el-radio-button>
                </el-radio-group>
                <el-select v-model="n.approvers" multiple filterable placeholder="选择审批人" size="small" style="flex:1">
                  <el-option v-for="u in users" :key="u.id" :label="u.nickname + '(' + u.username + ')'" :value="u.id" />
                </el-select>
              </div>
            </template>
            <template v-else>
              <div v-for="(b, bi) in n.branches" :key="bi" class="node-row">
                <el-input v-model="b.expr" :disabled="b.def" placeholder='条件，如 amount >= 50000' size="small" style="width:220px" />
                <el-checkbox v-model="b.def" size="small">默认分支</el-checkbox>
                <span style="font-size:12px;color:#999">跳到 L</span>
                <el-input-number v-model="b.goto" :min="1" :max="20" size="small" style="width:90px" />
                <el-button size="small" type="danger" plain @click="n.branches.splice(bi, 1)">删</el-button>
              </div>
              <el-button size="small" @click="n.branches.push({ expr: '', def: false, goto: i + 2 })">+分支</el-button>
            </template>
          </div>
          <el-button size="small" type="primary" plain @click="addNode">+节点</el-button>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
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

const rows = ref([]); const dlg = ref(false); const users = ref([])
const form = reactive({ status: 1 })
const editNodes = ref([])

function parseNodes(json) {
  try { return JSON.parse(json) } catch { return [] }
}
function branchesText(n) {
  return (n.branches || []).map(b => b.default ? '默认→L' + b.goto : (b.expr + ' →L' + b.goto)).join('；')
}
async function load() {
  rows.value = await http.get('/api/oa/flow/def/list')
  if (!users.value.length) {
    const p = await http.get('/api/user/page', { params: { pageNo: 1, pageSize: 100 } })
    users.value = p.records || []
  }
}
function addNode() {
  const next = editNodes.value.length + 1
  editNodes.value.push({ type: 'APPROVAL', name: '', signType: 'ANY', approvers: [], branches: [{ expr: '', def: false, goto: next }] })
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || { status: 1, bizType: 1 })
  if (row) {
    editNodes.value = parseNodes(row.nodes).map(n => ({
      type: n.type === 'CONDITION' ? 'CONDITION' : 'APPROVAL',
      name: n.name || '',
      signType: n.signType === 'ALL' ? 'ALL' : 'ANY',
      approvers: (n.approverUserIds || []).map(Number),
      branches: (n.branches || []).map(b => ({ expr: b.expr || '', def: !!b.default, goto: b.goto }))
    }))
  } else {
    editNodes.value = [{ type: 'APPROVAL', name: '', signType: 'ANY', approvers: [], branches: [] }]
  }
  dlg.value = true
}
async function save() {
  const nodes = editNodes.value.map((n, i) => {
    const base = { level: i + 1, name: n.name }
    if (n.type === 'CONDITION') {
      base.type = 'CONDITION'
      base.branches = n.branches.map(b => b.def ? { default: true, goto: b.goto } : { expr: b.expr, goto: b.goto })
    } else {
      base.signType = n.signType
      base.approverUserIds = n.approvers.map(Number)
    }
    return base
  })
  form.nodes = JSON.stringify(nodes)
  await http.post('/api/oa/flow/def/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function del(row) {
  await ElMessageBox.confirm(`删除流程「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/oa/flow/def/delete', { params: { id: row.id } })
  load()
}
onMounted(load)
</script>
<style scoped>
.node-box { border: 1px solid #ebeef5; border-radius: 6px; padding: 8px; margin-bottom: 8px; width: 100%; }
.node-row { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; flex-wrap: wrap; }
.lv { font-weight: bold; color: #409eff; width: 28px; }
.hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>

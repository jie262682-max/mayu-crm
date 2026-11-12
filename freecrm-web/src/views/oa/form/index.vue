<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-button type="primary" @click="openForm()">新建表单</el-button>
      <span style="margin-left:12px;color:#999;font-size:12px">定义字段后即可发起申请，申请自动进审批流（bizType=100）</span>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="表单名" width="160" />
      <el-table-column label="字段" min-width="300">
        <template #default="{ row }">{{ fieldsText(row.fields) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="success" v-if="row.status === 1" @click="goApply(row)">发起申请</el-button>
          <el-button link type="danger" @click="del(row)">删</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑表单' : '新建表单'" width="720px">
      <el-form label-width="90px">
        <el-form-item label="表单名"><el-input v-model="form.name" placeholder="如：差旅报销 / 用章申请" /></el-form-item>
        <el-form-item label="字段列表">
          <div v-for="(f, i) in fields" :key="i" style="display:flex;gap:6px;margin-bottom:6px;width:100%">
            <el-input v-model="f.key" placeholder="key（英文）" style="width:120px" />
            <el-input v-model="f.label" placeholder="标签" style="width:140px" />
            <el-select v-model="f.type" style="width:120px">
              <el-option v-for="t in ['text','textarea','number','date','select']" :key="t" :label="t" :value="t" />
            </el-select>
            <el-input v-if="f.type === 'select'" v-model="f.optionsStr" placeholder="选项,逗号分隔" style="flex:1" />
            <el-checkbox v-model="f.required" size="small">必填</el-checkbox>
            <el-button link type="danger" @click="fields.splice(i, 1)">删</el-button>
          </div>
          <el-button size="small" @click="fields.push({ key: '', label: '', type: 'text', required: false })">+字段</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyDlg" :title="`发起申请 - ${curForm?.name || ''}`" width="560px">
      <el-form label-width="110px">
        <el-form-item :label="f.label" v-for="f in parsedFields" :key="f.key">
          <el-select v-if="f.type === 'select'" v-model="formData[f.key]" style="width:100%">
            <el-option v-for="o in (f.options || [])" :key="o" :label="o" :value="o" />
          </el-select>
          <el-input-number v-else-if="f.type === 'number'" v-model="formData[f.key]" :min="0" style="width:100%" />
          <el-date-picker v-else-if="f.type === 'date'" v-model="formData[f.key]" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          <el-input v-else-if="f.type === 'textarea'" v-model="formData[f.key]" type="textarea" :rows="2" />
          <el-input v-else v-model="formData[f.key]" />
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="applyTitle" placeholder="摘要（用于待办展示）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDlg = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交（自动进审批）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([]); const dlg = ref(false); const form = reactive({})
const fields = ref([])
const applyDlg = ref(false); const curForm = ref(null); const formData = reactive({}); const applyTitle = ref('')

const parsedFields = computed(() => {
  try { return JSON.parse(curForm.value?.fields || '[]') } catch { return [] }
})

function fieldsText(json) {
  try { return JSON.parse(json).map(f => f.label + (f.required ? '*' : '')).join(' / ') } catch { return '-' }
}
async function load() {
  const p = await http.get('/api/oa/form/def/list')
  rows.value = p.records
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || {})
  try { fields.value = row ? JSON.parse(row.fields).map(f => ({ ...f, optionsStr: (f.options || []).join(',') })) : [{ key: '', label: '', type: 'text', required: false }]
  } catch { fields.value = [] }
  dlg.value = true
}
async function save() {
  const fs = fields.value.map(f => {
    const o = { key: f.key, label: f.label, type: f.type, required: !!f.required }
    if (f.type === 'select' && f.optionsStr) o.options = f.optionsStr.split(',').map(s => s.trim()).filter(Boolean)
    return o
  })
  form.fields = JSON.stringify(fs)
  await http.post('/api/oa/form/def/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function del(row) {
  await ElMessageBox.confirm(`删除表单「${row.name}」？已提交的申请不受影响`, '提示', { type: 'warning' })
  await http.delete('/api/oa/form/def/delete', { params: { id: row.id } })
  load()
}
function goApply(row) {
  curForm.value = row
  Object.keys(formData).forEach(k => delete formData[k])
  applyTitle.value = ''
  applyDlg.value = true
}
async function submitApply() {
  const r = await http.post('/api/oa/form/apply', {
    formId: curForm.value.id, title: applyTitle.value, formData: JSON.stringify(formData)
  })
  ElMessage.success(r.flowStarted ? '已提交审批' : '未配置流程，已直接通过')
  applyDlg.value = false
}
onMounted(load)
</script>

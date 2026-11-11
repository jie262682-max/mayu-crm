<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="线索名称/联系人" style="width:220px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建线索</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="线索名称" min-width="140" />
      <el-table-column prop="contactName" label="联系人" width="110" />
      <el-table-column prop="mobile" label="手机" width="130" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ sourceName(row.source) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.transformStatus === 1 ? 'success' : 'info'">
            {{ row.transformStatus === 1 ? '已转化' : '跟进中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nextContactTime" label="下次联系" width="160" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button v-if="row.transformStatus !== 1" link type="success" @click="transform(row)">转客户</el-button>
          <el-button link type="warning" @click="openFollow(row)">跟进</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑线索' : '新建线索'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="线索名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.source" style="width:100%">
            <el-option v-for="(n, i) in SOURCES" :key="i" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次联系">
          <el-date-picker v-model="form.nextContactTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followDlg" title="跟进记录" width="640px">
      <el-timeline style="padding-left:4px">
        <el-timeline-item v-for="f in follows" :key="f.id" :timestamp="f.createTime" placement="top">
          <b>{{ typeName(f.type) }}</b> — {{ f.content }}
          <div v-if="f.nextTime" style="color:#999;font-size:12px">下次：{{ f.nextTime }}</div>
        </el-timeline-item>
        <el-timeline-item v-if="follows.length === 0">暂无跟进</el-timeline-item>
      </el-timeline>
      <el-divider>写跟进</el-divider>
      <el-form :inline="true">
        <el-form-item>
          <el-select v-model="nf.type" style="width:110px" placeholder="方式">
            <el-option v-for="(n, i) in TYPES" :key="i" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker v-model="nf.nextTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="下次联系" />
        </el-form-item>
      </el-form>
      <el-input v-model="nf.content" type="textarea" :rows="2" placeholder="跟进内容" />
      <template #footer>
        <el-button @click="followDlg = false">关闭</el-button>
        <el-button type="primary" @click="saveFollow">提交跟进</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const SOURCES = ['广告', '搜索引擎', '转介绍', '电话陌拜', '其他']
const TYPES = ['电话', '微信', '上门', '邮件', '其他']

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')
const dlg = ref(false)
const form = reactive({})
const followDlg = ref(false)
const follows = ref([])
const nf = reactive({ type: 1, content: '', nextTime: '' })
let curLead = null

const sourceName = s => SOURCES[s - 1] || '-'
const typeName = t => TYPES[t - 1] || '其他'

async function load() {
  const p = await http.get('/api/crm/lead/page', { params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || { source: 5 })
  dlg.value = true
}
async function save() {
  await http.post('/api/crm/lead/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function transform(row) {
  await ElMessageBox.confirm(`将线索「${row.name}」转化为客户？转化后不可逆。`, '转化确认', { type: 'warning' })
  const cid = await http.put(`/api/crm/lead/transform?id=${row.id}`)
  ElMessage.success(`已转化，客户ID：${cid}`)
  load()
}
async function del(row) {
  await ElMessageBox.confirm(`删除线索「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete('/api/crm/lead/delete', { params: { id: row.id } })
  ElMessage.success('已删除'); load()
}
async function openFollow(row) {
  curLead = row
  const p = await http.get('/api/crm/follow-up/page', { params: { bizType: 1, bizId: row.id, pageSize: 50 } })
  follows.value = p.records
  followDlg.value = true
}
async function saveFollow() {
  if (!nf.content) return ElMessage.warning('填写跟进内容')
  await http.post('/api/crm/follow-up/save', { bizType: 1, bizId: curLead.id, type: nf.type, content: nf.content, nextTime: nf.nextTime || null })
  ElMessage.success('已记录'); nf.content = ''; nf.nextTime = ''
  openFollow(curLead)
}
onMounted(load)
</script>

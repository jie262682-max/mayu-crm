<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px">
      <el-input v-model="kw" placeholder="客户名称" style="width:220px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openForm()">新建客户</el-button>
      <el-button plain @click="openDup">查重</el-button>
      <el-dropdown style="margin-left:2px">
        <el-button plain>导入/导出 <el-icon><ArrowDown /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="download('/api/crm/excel/template?type=customer')">下载导入模板</el-dropdown-item>
            <el-dropdown-item @click="pickFile">导入客户 Excel</el-dropdown-item>
            <el-dropdown-item divided @click="download('/api/crm/excel/export?type=customer&keyword=' + (kw || ''))">导出客户</el-dropdown-item>
            <el-dropdown-item @click="download('/api/crm/excel/export?type=lead')">导出线索</el-dropdown-item>
            <el-dropdown-item @click="download('/api/crm/excel/export?type=contract')">导出合同</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <input ref="fileEl" type="file" accept=".xlsx" style="display:none" @change="doImport" />
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="客户名称" min-width="150" />
      <el-table-column prop="mobile" label="手机" width="130" />
      <el-table-column label="等级" width="80">
        <template #default="{ row }">{{ ['A', 'B', 'C', 'D'][row.level - 1] || '-' }}</template>
      </el-table-column>
      <el-table-column label="成交" width="80">
        <template #default="{ row }">
          <el-tag :type="row.dealStatus === 1 ? 'success' : 'info'" size="small">{{ row.dealStatus === 1 ? '已成交' : '未成交' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nextContactTime" label="下次联系" width="160" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="success" @click="openContacts(row)">联系人</el-button>
          <el-button link type="warning" @click="openFollow(row)">跟进</el-button>
          <el-button link type="danger" @click="putPool(row)">放公海</el-button>
          <el-button link @click="openFiles(row)">资料</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dlg" :title="form.id ? '编辑客户' : '新建客户'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="客户名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.mobile" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.level" style="width:100%">
            <el-option v-for="(n, i) in ['A', 'B', 'C', 'D']" :key="n" :label="n" :value="i + 1" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fileDlg" :title="`客户资料 - ${curCustomer2?.name || ''}`" width="680px">
      <el-upload
        :auto-upload="false"
        ref="uploadEl"
        multiple
        :on-change="onFileChange"
        :show-file-list="false"
      >
        <el-button type="primary">选择文件（可多选）</el-button>
        <template #tip>
          <div class="el-upload__tip">单个不超过 20MB；点击列表「上传」提交所选 {{ pendingFiles.length }} 个文件</div>
        </template>
      </el-upload>
      <el-button v-if="pendingFiles.length" type="success" style="margin-top:8px" :loading="uploading" @click="doUpload">上传 {{ pendingFiles.length }} 个文件</el-button>

      <el-table :data="fileRows" border size="small" style="margin-top:12px">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ fmtSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadFile(row)">下载</el-button>
            <el-button link type="danger" @click="delFile(row)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="dupDlg" title="客户查重 / 合并" width="720px">
      <div style="display:flex;gap:8px;margin-bottom:10px">
        <el-input v-model="dupName" placeholder="客户名称" style="width:220px" />
        <el-input v-model="dupMobile" placeholder="手机号（可空）" style="width:180px" />
        <el-button type="primary" @click="checkDup">查重</el-button>
      </div>
      <el-table :data="dupRows" border size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="mobile" label="手机" width="120" />
        <el-table-column label="成交" width="70">
          <template #default="{ row }">{{ row.dealStatus === 1 ? '已成交' : '未成交' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-radio-group v-model="mergeMain" size="small">
              <el-radio-button :value="row.id">{{ mergeMain === row.id ? '保留此客户' : '设为保留' }}</el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="dupRows.length >= 2" style="margin-top:10px;color:#999;font-size:13px">
        选择要保留的客户后，点其他客户的「并入」将其联系人/跟进/商机/合同全部迁移并删除。
      </div>
      <div v-if="dupRows.length >= 2 && mergeMain" style="margin-top:8px;display:flex;gap:8px;flex-wrap:wrap">
        <el-button v-for="d in dupRows.filter(r => r.id !== mergeMain)" :key="d.id" size="small" type="warning" plain
                   @click="doMerge(d)">「{{ d.name }}」并入保留客户</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="followDlg" title="跟进记录" width="680px">
      <el-timeline style="padding-left:4px">
        <el-timeline-item v-for="f in follows" :key="f.id" :timestamp="f.createTime" placement="top">
          <b>{{ ['电话', '微信', '上门', '邮件', '其他'][f.type - 1] || '其他' }}</b> — {{ f.content }}
          <div v-if="filesOf(f.id).length" style="margin-top:4px">
            <el-tag v-for="a in filesOf(f.id)" :key="a.id" size="small" type="info" style="cursor:pointer;margin-right:6px"
                    @click="downloadFile(a)">📎 {{ a.fileName }}</el-tag>
          </div>
        </el-timeline-item>
        <el-timeline-item v-if="follows.length === 0">暂无跟进</el-timeline-item>
      </el-timeline>
      <el-divider>写跟进（可带资料附件）</el-divider>
      <el-input v-model="nfContent" type="textarea" :rows="2" placeholder="跟进内容" />
      <div style="margin-top:8px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">
        <input ref="followFileEl" type="file" multiple style="display:none" @change="onFollowFiles" />
        <el-button plain size="small" @click="followFileEl?.click()">📎 添加附件（可多选）</el-button>
        <el-tag v-for="(f, i) in nfFiles" :key="i" closable size="small" @close="nfFiles.splice(i, 1)">{{ f.name }}</el-tag>
      </div>
      <template #footer>
        <el-button @click="followDlg = false">关闭</el-button>
        <el-button type="primary" :loading="followSaving" @click="saveFollow">提交跟进</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contactDlg" :title="`联系人 - ${curCustomer?.name || ''}`" width="680px">
      <el-button size="small" type="primary" plain style="margin-bottom:8px" @click="newContact">新增联系人</el-button>
      <el-table :data="contacts" border size="small">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="mobile" label="手机" width="120" />
        <el-table-column prop="post" label="职务" width="110" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">{{ ['决策者', '影响者', '使用者', '教练'][row.decisionRole - 1] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="140" />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" @click="delContact(row)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="contactFormDlg" title="联系人" width="420px" append-to-body>
        <el-form :model="contactForm" label-width="70px" v-if="contactForm">
          <el-form-item label="姓名"><el-input v-model="contactForm.name" /></el-form-item>
          <el-form-item label="手机"><el-input v-model="contactForm.mobile" /></el-form-item>
          <el-form-item label="职务"><el-input v-model="contactForm.post" /></el-form-item>
          <el-form-item label="角色">
            <el-select v-model="contactForm.decisionRole" style="width:100%">
              <el-option v-for="(n, i) in ['决策者', '影响者', '使用者', '教练']" :key="n" :label="n" :value="i + 1" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="contactFormDlg = false">取消</el-button>
          <el-button type="primary" @click="saveContact">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const fileEl = ref()
const fileDlg = ref(false); const fileRows = ref([]); const curCustomer2 = ref(null)
const pendingFiles = ref([]); const uploading = ref(false); const uploadEl = ref()

function openFiles(row) {
  curCustomer2.value = row
  pendingFiles.value = []
  loadFiles()
  fileDlg.value = true
}
async function loadFiles() {
  fileRows.value = await http.get('/api/crm/customer/file/list', { params: { customerId: curCustomer2.value.id } })
}
function onFileChange(file) {
  if (file.status === 'ready') pendingFiles.value.push(file.raw)
}
async function doUpload() {
  if (!pendingFiles.value.length) return
  uploading.value = true
  try {
    const fd = new FormData()
    pendingFiles.value.forEach(f => fd.append('files', f))
    await http.post('/api/crm/customer/file/upload?customerId=' + curCustomer2.value.id, fd,
      { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('上传成功')
    pendingFiles.value = []
    uploadEl.value?.clearFiles()
    loadFiles()
  } finally {
    uploading.value = false
  }
}
function fmtSize(n) {
  if (!n) return '-'
  return n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : (n / 1024).toFixed(0) + ' KB'
}
function downloadFile(row) {
  const token = localStorage.getItem('token')
  fetch('/api/crm/customer/file/download?id=' + row.id, { headers: { Authorization: token } })
    .then(r => { if (!r.ok) throw new Error(); return r.blob() })
    .then(b => {
      const u = URL.createObjectURL(b)
      const a = document.createElement('a')
      a.href = u; a.download = row.fileName; a.click()
      URL.revokeObjectURL(u)
    })
    .catch(() => ElMessage.error('下载失败'))
}
async function delFile(row) {
  await ElMessageBox.confirm(`删除资料「${row.fileName}」？`, '提示', { type: 'warning' })
  await http.delete('/api/crm/customer/file/delete', { params: { id: row.id } })
  loadFiles()
}
const dupDlg = ref(false); const dupName = ref(''); const dupMobile = ref(''); const dupRows = ref([]); const mergeMain = ref(null)

function openDup() { dupDlg.value = true }
async function checkDup() {
  dupRows.value = await http.get('/api/crm/customer/check-duplicate', {
    params: { name: dupName.value || undefined, mobile: dupMobile.value || undefined }
  })
  if (dupRows.value.length === 0) ElMessage.success('未发现重复客户')
}
async function doMerge(dup) {
  await ElMessageBox.confirm(`将「${dup.name}」的全部业务数据并入保留客户后删除该客户？`, '确认合并', { type: 'warning' })
  const r = await http.post('/api/crm/customer/merge', null, { params: { mainId: mergeMain.value, dupId: dup.id } })
  ElMessage.success(`合并完成：联系人 ${r.contacts} 条、跟进 ${r.followUps} 条、商机 ${r.opportunities} 个、合同 ${r.contracts} 份`)
  checkDup(); load()
}
function download(url) {
  const token = localStorage.getItem('token')
  const full = url + (url.includes('?') ? '&' : '?') + '_t=' + Date.now()
  // 带 token 下载：fetch 转 blob
  fetch(full, { headers: { Authorization: token } }).then(r => {
    if (!r.ok) throw new Error('下载失败')
    return r.blob()
  }).then(b => {
    const u = URL.createObjectURL(b)
    const link = document.createElement('a')
    link.href = u
    link.download = (url.includes('template') ? 'import-template' : 'export') + '.xlsx'
    link.click()
    URL.revokeObjectURL(u)
  }).catch(() => ElMessage.error('下载失败'))
}
function pickFile() { fileEl.value.click() }
async function doImport(e) {
  const f = e.target.files[0]
  if (!f) return
  const fd = new FormData()
  fd.append('file', f)
  const r = await http.post('/api/crm/excel/import?type=customer', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  ElMessageBox.alert(`成功 ${r.ok} 条，跳过（空行/重名/示例） ${r.skipped} 条` + (r.errors?.length ? `<br><br>${r.errors.join('<br>')}` : ''), '导入结果', { dangerouslyUseHTMLString: true })
  load()
  e.target.value = ''
}

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')
const dlg = ref(false); const form = reactive({})
const followDlg = ref(false); const follows = ref([]); const nfContent = ref('')
const nfFiles = ref([]); const followFileEl = ref(); const followSaving = ref(false); const followFilesAll = ref([])
const contactDlg = ref(false); const contacts = ref([]); const curCustomer = ref(null)
const contactFormDlg = ref(false); const contactForm = ref(null)
let followTarget = null

async function load() {
  const p = await http.get('/api/crm/customer/page', { params: { pageNo: pageNo.value, pageSize: 10, keyword: kw.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
function openForm(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row || {})
  dlg.value = true
}
async function save() {
  await http.post('/api/crm/customer/save', form)
  ElMessage.success('已保存'); dlg.value = false; load()
}
async function putPool(row) {
  await ElMessageBox.confirm(`将「${row.name}」放入公海？团队将解散。`, '提示', { type: 'warning' })
  await http.put(`/api/crm/customer/put-pool?id=${row.id}`)
  ElMessage.success('已放入公海'); load()
}
async function openFollow(row) {
  followTarget = row
  const p = await http.get('/api/crm/follow-up/page', { params: { bizType: 2, bizId: row.id, pageSize: 50 } })
  follows.value = p.records
  try {
    followFilesAll.value = await http.get('/api/crm/customer/file/list', { params: { customerId: row.id } })
  } catch (e) { followFilesAll.value = [] }
  nfFiles.value = []
  followDlg.value = true
}
function filesOf(followId) {
  return followFilesAll.value.filter(f => f.followUpId === followId)
}
function onFollowFiles(e) {
  nfFiles.value.push(...Array.from(e.target.files || []))
  e.target.value = ''
}
async function saveFollow() {
  if (!nfContent.value) return ElMessage.warning('填写跟进内容')
  followSaving.value = true
  try {
    const saved = await http.post('/api/crm/follow-up/save', { bizType: 2, bizId: followTarget.id, type: 1, content: nfContent.value })
    // 跟进附件：先存跟进拿 ID，再带 followUpId 上传
    if (nfFiles.value.length) {
      const fd = new FormData()
      nfFiles.value.forEach(f => fd.append('files', f))
      await http.post('/api/crm/customer/file/upload?customerId=' + followTarget.id + '&followUpId=' + saved, fd,
        { headers: { 'Content-Type': 'multipart/form-data' } })
    }
    ElMessage.success('已记录' + (nfFiles.value.length ? '（含附件 ' + nfFiles.value.length + ' 个）' : ''))
    nfContent.value = ''
    openFollow(followTarget)
  } finally {
    followSaving.value = false
  }
}
async function openContacts(row) {
  curCustomer.value = row
  const p = await http.get('/api/crm/contact/page', { params: { customerId: row.id, pageSize: 50 } })
  contacts.value = p.records
  contactDlg.value = true
}
function newContact() {
  contactForm.value = { customerId: curCustomer.value?.id, decisionRole: 3 }
  contactFormDlg.value = true
}
async function saveContact() {
  await http.post('/api/crm/contact/save', contactForm.value)
  ElMessage.success('已保存'); contactFormDlg.value = false
  openContacts(curCustomer.value)
}
async function delContact(row) {
  await http.delete('/api/crm/contact/delete', { params: { id: row.id } })
  openContacts(curCustomer.value)
}
onMounted(load)
</script>

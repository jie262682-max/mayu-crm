<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane label="公海回收规则" name="pool">
        <el-form label-width="220px" style="max-width:640px;margin-top:12px">
          <el-form-item label="自动回收开关">
            <el-switch v-model="pool.enabled" active-value="1" inactive-value="0" />
            <span class="muted" style="margin-left:12px">关闭后每小时定时任务空跑，也可用下面按钮手动触发</span>
          </el-form-item>
          <el-form-item label="掉保回收（天未跟进入公海）">
            <el-input-number v-model="pool.recycleDays" :min="1" :max="365" />
          </el-form-item>
          <el-form-item label="超期回收（天未成交入公海）">
            <el-input-number v-model="pool.expireDays" :min="1" :max="730" />
          </el-form-item>
          <el-form-item label="领取保护期（天）">
            <el-input-number v-model="pool.protectDays" :min="0" :max="90" />
            <span class="muted" style="margin-left:12px">客户被领取/转移后 N 天内不被回收</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="savePool">保存规则</el-button>
            <el-button plain @click="loadPreview" :loading="previewing">预览将回收客户</el-button>
            <el-button type="warning" plain @click="recycleNow" :loading="recycling">立即回收</el-button>
          </el-form-item>
          <el-form-item>
            <span class="muted">每小时整点自动执行；已成交/已锁定客户不回收，回收后给原负责人发站内信</span>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">回收预览（{{ previewRows.length }} 条，不执行任何操作）</el-divider>
        <el-table :data="previewRows" border size="small" max-height="360">
          <el-table-column prop="customerId" label="客户ID" width="80" />
          <el-table-column prop="customerName" label="客户名" min-width="160" />
          <el-table-column label="负责人" width="110">
            <template #default="{ row }">{{ userMap[row.ownerUserId] || ('用户' + row.ownerUserId) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建日期" width="110" />
          <el-table-column prop="lastContactTime" label="最近跟进" width="110">
            <template #default="{ row }">{{ row.lastContactTime || '从未跟进' }}</template>
          </el-table-column>
          <el-table-column label="命中规则" min-width="240">
            <template #default="{ row }">
              <el-tag v-for="r in row.reasons" :key="r" type="warning" size="small" style="margin-right:6px">{{ r }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-if="recycleResult" :title="recycleResult" type="success" style="margin-top:10px" :closable="false" />
      </el-tab-pane>

      <el-tab-pane label="业务提醒" name="remind">
        <el-form label-width="220px" style="max-width:640px;margin-top:12px">
          <el-form-item label="合同到期提前提醒（天）">
            <el-input-number v-model="remind.contractDays" :min="1" :max="365" />
            <span class="muted" style="margin-left:12px">到期前 N 天开始每天提醒负责人；逾期未收完款持续提醒</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveRemind">保存</el-button>
            <span class="muted" style="margin-left:12px">每天 08:15 执行；跟进到期提醒固定每天 08:05 发送，无需配置</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="阿里云短信" name="sms">
        <el-form label-width="180px" style="max-width:640px;margin-top:12px">
          <el-form-item label="启用短信">
            <el-switch v-model="sms.enabled" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item label="AccessKeyId">
            <el-input v-model="sms.accessKeyId" placeholder="阿里云 RAM AccessKey" style="width:360px" />
          </el-form-item>
          <el-form-item label="AccessKeySecret">
            <el-input v-model="sms.accessSecret" type="password" show-password placeholder="留 ****** 表示不修改" style="width:360px" />
          </el-form-item>
          <el-form-item label="短信签名">
            <el-input v-model="sms.signName" placeholder="如：某某科技" style="width:360px" />
          </el-form-item>
          <el-form-item label="模板 Code">
            <el-input v-model="sms.templateCode" placeholder="如：SMS_123456789" style="width:360px" />
          </el-form-item>
          <el-form-item label="审批待办短信通知">
            <el-switch v-model="sms.notifyTodo" active-value="1" inactive-value="0" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveSms">保存配置</el-button>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">测试发送</el-divider>
        <div style="display:flex;gap:8px;max-width:640px">
          <el-input v-model="testMobile" placeholder="接收手机号" style="width:200px" />
          <el-input v-model="testParam" placeholder='模板变量，如 title=测试审批（可空）' style="width:280px" />
          <el-button type="primary" plain @click="sendTest">发送测试</el-button>
        </div>
        <el-alert v-if="testResult" :title="testResult" :type="testOk ? 'success' : 'error'" style="margin-top:10px;max-width:640px" :closable="false" />

        <el-divider content-position="left">发送记录</el-divider>
        <el-table :data="logs" border size="small">
          <el-table-column prop="mobile" label="手机号" width="130" />
          <el-table-column prop="template" label="模板" width="140" />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMsg" label="错误" min-width="180" />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
        <el-pagination style="margin-top:10px;justify-content:flex-end" v-model:current-page="logPage"
                       :page-size="10" :total="logTotal" layout="total, prev, pager, next" @current-change="loadLogs" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tab = ref('pool')
const pool = reactive({ enabled: '1', recycleDays: 30, expireDays: 90, protectDays: 7 })
const remind = reactive({ contractDays: 30 })
const sms = reactive({ enabled: '0', accessKeyId: '', accessSecret: '', signName: '', templateCode: '', notifyTodo: '1' })
const previewRows = ref([]); const previewing = ref(false)
const recycling = ref(false); const recycleResult = ref('')
const userMap = ref({})
const testMobile = ref(''); const testParam = ref(''); const testResult = ref(''); const testOk = ref(false)
const logs = ref([]); const logTotal = ref(0); const logPage = ref(1)

async function loadAll() {
  const cfg = await http.get('/api/config/list')
  const m = {}
  cfg.forEach(c => { m[c.configKey] = c.configValue })
  pool.recycleDays = Number(m['pool.recycle.days'] || 30)
  pool.expireDays = Number(m['pool.expire.days'] || 90)
  pool.protectDays = Number(m['pool.protect.days'] ?? 7)
  pool.enabled = m['pool.recycle.enabled'] ?? '1'
  remind.contractDays = Number(m['contract.remind.days'] || 30)
  const users = await http.get('/api/user/simple-list')
  const um = {}; (users || []).forEach(u => { um[u.id] = u.nickname })
  userMap.value = um
  const sc = await http.get('/api/sms/config')
  Object.assign(sms, sc)
  loadLogs()
}
async function savePool() {
  await http.post('/api/config/save', {
    'pool.recycle.enabled': pool.enabled,
    'pool.recycle.days': String(pool.recycleDays),
    'pool.expire.days': String(pool.expireDays),
    'pool.protect.days': String(pool.protectDays)
  })
  ElMessage.success('回收规则已保存（立即生效，无需重启）')
}
async function saveRemind() {
  await http.post('/api/config/save', { 'contract.remind.days': String(remind.contractDays) })
  ElMessage.success('提醒配置已保存（立即生效，无需重启）')
}
async function loadPreview() {
  previewing.value = true
  try {
    previewRows.value = await http.get('/api/crm/customer/pool/recycle-preview')
    if (!previewRows.value.length) ElMessage.info('按当前规则暂无待回收客户')
  } finally { previewing.value = false }
}
async function recycleNow() {
  await ElMessageBox.confirm('立即按当前规则执行一轮公海回收？回收后客户进入公海，原负责人将收到站内信。', '确认回收', { type: 'warning' })
  recycling.value = true
  try {
    const r = await http.post('/api/crm/customer/pool/recycle-now')
    recycleResult.value = r.skipped
      ? '自动回收开关已关闭，未执行（可在上方开启后重试）'
      : `回收完成：掉保回收 ${r.noContact} 个，超期回收 ${r.expire} 个`
    loadPreview()
  } finally { recycling.value = false }
}
async function saveSms() {
  await http.post('/api/sms/config', {
    'sms.enabled': sms.enabled, 'sms.accessKeyId': sms.accessKeyId, 'sms.accessSecret': sms.accessSecret,
    'sms.signName': sms.signName, 'sms.templateCode': sms.templateCode, 'sms.notify.todo': sms.notifyTodo
  })
  ElMessage.success('短信配置已保存（立即生效，无需重启）')
  const sc = await http.get('/api/sms/config')
  Object.assign(sms, sc)
}
async function sendTest() {
  if (!testMobile.value) return
  const r = await http.post('/api/sms/test', null, { params: { mobile: testMobile.value, paramJson: testParam.value || undefined } })
  testOk.value = true
  testResult.value = r
  loadLogs()
}
async function loadLogs() {
  const p = await http.get('/api/sms/log/page', { params: { pageNo: logPage.value, pageSize: 10 } })
  logs.value = p.records; logTotal.value = Number(p.total)
}
onMounted(loadAll)
</script>
<style scoped>
.muted { color: #999; font-size: 12px; }
</style>

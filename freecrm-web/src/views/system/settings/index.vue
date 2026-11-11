<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane label="公海回收规则" name="pool">
        <el-form label-width="220px" style="max-width:640px;margin-top:12px">
          <el-form-item label="掉保回收（天未跟进入公海）">
            <el-input-number v-model="pool.recycleDays" :min="1" :max="365" />
          </el-form-item>
          <el-form-item label="超期回收（天未成交入公海）">
            <el-input-number v-model="pool.expireDays" :min="1" :max="730" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="savePool">保存规则</el-button>
            <span class="muted" style="margin-left:12px">每小时整点自动执行；已成交/已锁定客户不回收，回收后给原负责人发站内信</span>
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
import { ElMessage } from 'element-plus'

const tab = ref('pool')
const pool = reactive({ recycleDays: 30, expireDays: 90 })
const sms = reactive({ enabled: '0', accessKeyId: '', accessSecret: '', signName: '', templateCode: '', notifyTodo: '1' })
const testMobile = ref(''); const testParam = ref(''); const testResult = ref(''); const testOk = ref(false)
const logs = ref([]); const logTotal = ref(0); const logPage = ref(1)

async function loadAll() {
  const cfg = await http.get('/api/config/list')
  const m = {}
  cfg.forEach(c => { m[c.configKey] = c.configValue })
  pool.recycleDays = Number(m['pool.recycle.days'] || 30)
  pool.expireDays = Number(m['pool.expire.days'] || 90)
  const sc = await http.get('/api/sms/config')
  Object.assign(sms, sc)
  loadLogs()
}
async function savePool() {
  await http.post('/api/config/save', { 'pool.recycle.days': String(pool.recycleDays), 'pool.expire.days': String(pool.expireDays) })
  ElMessage.success('回收规则已保存')
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

<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px">
      <el-input v-model="kw" placeholder="合同ID" style="width:140px" clearable @keyup.enter="load" />
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="contractId" label="合同ID" width="90" />
      <el-table-column prop="returnTime" label="回款日期" width="110" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column label="方式" width="100">
        <template #default="{ row }">{{ ['对公转账', '现金', '微信', '支付宝', '其他'][row.method - 1] || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['warning', 'success', 'danger'][row.auditStatus]" size="small">
            {{ ['待审', '通过', '驳回'][row.auditStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" />
      <el-table-column prop="createTime" label="录入时间" width="160" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import http from '../../../api/request'

const rows = ref([]); const total = ref(0); const pageNo = ref(1); const kw = ref('')

async function load() {
  const p = await http.get('/api/crm/contract/payment/page', {
    params: { pageNo: pageNo.value, pageSize: 10, contractId: kw.value || undefined }
  })
  rows.value = p.records; total.value = Number(p.total)
}
onMounted(load)
</script>

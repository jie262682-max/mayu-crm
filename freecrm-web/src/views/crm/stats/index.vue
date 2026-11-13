<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6" v-for="c in cards1" :key="c.label">
        <el-card shadow="hover">
          <div style="color:#999;font-size:13px">{{ c.label }}</div>
          <div style="font-size:26px;font-weight:bold;margin-top:6px">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="c in cards2" :key="c.label">
        <el-card shadow="hover">
          <div style="color:#999;font-size:13px">{{ c.label }}</div>
          <div style="font-size:22px;font-weight:bold;margin-top:6px" :style="c.color ? { color: c.color } : {}">
            {{ c.value }}<span v-if="c.sub" style="font-size:12px;color:#999;font-weight:normal;margin-left:4px">{{ c.sub }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>近 30 天新增客户</template>
          <div ref="trendEl" style="height:240px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>销售排行（已审合同额 / 回款额）</template>
          <div ref="rankEl" style="height:240px" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>近 6 个月签约 vs 回款（元）</template>
          <div ref="payTrendEl" style="height:240px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>客户来源分布
            <el-radio-group v-model="distDim" size="small" style="margin-left:12px" @change="loadDist">
              <el-radio-button value="source">来源</el-radio-button>
              <el-radio-button value="industry">行业</el-radio-button>
            </el-radio-group>
          </template>
          <div ref="distEl" style="height:240px" />
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never" style="margin-top:12px">
      <template #header>商机漏斗（进行中）</template>
      <el-table :data="funnel" border size="small">
        <el-table-column prop="stageId" label="阶段ID" width="90" />
        <el-table-column prop="count" label="商机数" width="100" />
        <el-table-column prop="amount" label="金额合计" width="160" />
      </el-table>
    </el-card>
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import http from '../../../api/request'

const cards1 = ref([
  { label: '我的客户', value: '-' }, { label: '进行中商机金额', value: '-' },
  { label: '合同总额', value: '-' }, { label: '已回款', value: '-' }
])
const cards2 = ref([
  { label: '本月新增客户', value: '-' }, { label: '本月签约额（已审）', value: '-' },
  { label: '本月回款额', value: '-' }, { label: '30 天内到期合同', value: '-', sub: '' }
])
const funnel = ref([])
const distDim = ref('source')
const trendEl = ref(); const rankEl = ref(); const payTrendEl = ref(); const distEl = ref()
// 字典与 crm_customer.source 注释对齐；industry 为自由字典，超界回退 #key
const SOURCE_NAMES = ['未知', '广告', '搜索引擎', '转介绍', '电话陌拜', '其他']
const INDUSTRY_NAMES = ['未知', '互联网/IT', '智能制造', '机械设备', '物流运输', '医疗健康']

onMounted(async () => {
  const [s, users] = await Promise.all([
    http.get('/api/crm/stats/summary'),
    http.get('/api/user/simple-list').catch(() => [])
  ])
  cards1.value[0].value = s.customers
  cards1.value[1].value = s.oppAmount
  cards1.value[2].value = s.contractAmount
  cards1.value[3].value = s.received
  cards2.value[0].value = s.monthNewCustomers
  cards2.value[1].value = s.monthContractAmount
  cards2.value[2].value = s.monthReceived
  cards2.value[3].value = s.expiringContracts
  cards2.value[3].sub = s.overdueContracts > 0 ? `逾期未收完 ${s.overdueContracts} 个` : ''
  if (s.overdueContracts > 0) cards2.value[3].color = '#E6A23C'

  const trend = await http.get('/api/crm/stats/customer-trend', { params: { days: 30 } })
  echarts.init(trendEl.value).setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(p => p.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: trend.map(p => p.count) }]
  })

  const name = id => {
    const u = (users || []).find(x => x.id === id)
    return u ? u.nickname : ('用户' + id)
  }
  const rank = await http.get('/api/crm/stats/sales-rank')
  echarts.init(rankEl.value).setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 70, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rank.map(r => name(r.userId)).reverse() },
    series: [
      { name: '合同额', type: 'bar', data: rank.map(r => r.amount).reverse(), itemStyle: { color: '#409EFF' } },
      { name: '回款额', type: 'bar', data: rank.map(r => r.received).reverse(), itemStyle: { color: '#67C23A' } }
    ]
  })

  const pt = await http.get('/api/crm/stats/payment-trend', { params: { months: 6 } })
  echarts.init(payTrendEl.value).setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 60, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: pt.map(p => p.month) },
    yAxis: { type: 'value' },
    series: [
      { name: '签约额', type: 'bar', data: pt.map(p => p.contractAmount), itemStyle: { color: '#409EFF' } },
      { name: '回款额', type: 'line', smooth: true, data: pt.map(p => p.receivedAmount),
        itemStyle: { color: '#67C23A' }, lineStyle: { width: 2 } }
    ]
  })

  funnel.value = await http.get('/api/crm/stats/opp-funnel')
  loadDist()
})

async function loadDist() {
  const d = await http.get('/api/crm/stats/customer-dist', { params: { dim: distDim.value } })
  const names = distDim.value === 'source' ? SOURCE_NAMES : INDUSTRY_NAMES
  echarts.init(distEl.value).setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
    legend: { orient: 'vertical', right: 0, top: 'middle', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['35%', '65%'], center: ['38%', '50%'],
      label: { show: false },
      data: d.map(x => ({ name: names[x.key] || ('#' + x.key), value: x.count }))
    }]
  }, true)
}
</script>

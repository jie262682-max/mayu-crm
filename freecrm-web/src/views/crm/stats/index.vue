<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <el-row :gutter="12" style="margin-bottom:16px">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="hover">
          <div style="color:#999;font-size:13px">{{ c.label }}</div>
          <div style="font-size:26px;font-weight:bold;margin-top:6px">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>近 30 天新增客户</template>
          <div ref="trendEl" style="height:260px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>销售排行（合同额）</template>
          <div ref="rankEl" style="height:260px" />
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

const cards = ref([
  { label: '我的客户', value: '-' }, { label: '进行中商机金额', value: '-' },
  { label: '合同总额', value: '-' }, { label: '已回款', value: '-' }
])
const funnel = ref([])
const trendEl = ref(); const rankEl = ref()

onMounted(async () => {
  const s = await http.get('/api/crm/stats/summary')
  cards.value[0].value = s.customers
  cards.value[1].value = s.oppAmount
  cards.value[2].value = s.contractAmount
  cards.value[3].value = s.received

  const trend = await http.get('/api/crm/stats/customer-trend', { params: { days: 30 } })
  echarts.init(trendEl.value).setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(p => p.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: trend.map(p => p.count) }]
  })

  const rank = await http.get('/api/crm/stats/sales-rank')
  echarts.init(rankEl.value).setOption({
    tooltip: {},
    grid: { left: 70, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rank.map(r => '用户' + r.userId).reverse() },
    series: [{ type: 'bar', data: rank.map(r => r.amount).reverse(), itemStyle: { color: '#409EFF' } }]
  })

  funnel.value = await http.get('/api/crm/stats/opp-funnel')
})
</script>

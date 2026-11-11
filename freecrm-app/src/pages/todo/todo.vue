<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view v-for="t in tasks" :key="t.id" class="card" @click="go(t)">
      <view class="row">
        <text class="biz">{{ titles[t.instanceId] || '审批任务' }}</text>
        <text class="muted">第 {{ t.nodeLevel }} 级</text>
      </view>
      <view class="muted" style="margin-top:8rpx">任务 #{{ t.id }} · 点击查看详情并处理</view>
    </view>
    <view v-if="!loading && tasks.length === 0" class="muted empty">没有待办，喝杯咖啡 ☕</view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { tasks: [], titles: {}, loading: false }
  },
  onShow() {
    if (uni.getStorageSync('token')) this.load()
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    async load() {
      this.loading = true
      this.titles = {}
      try {
        const tasks = await http.get('/api/oa/flow/todo')
        this.tasks = tasks
        // 实例 bizTitle 已含业务摘要（合同号 / 请假天数等）
        for (const t of tasks) {
          try {
            const inst = await http.get('/api/oa/flow/instance-by-id', { instanceId: t.instanceId })
            if (inst) this.titles[t.instanceId] = inst.bizTitle
          } catch (e) { /* 单个失败不影响列表 */ }
        }
      } finally {
        this.loading = false
      }
    },
    go(t) {
      uni.navigateTo({ url: `/pages/approval/detail?taskId=${t.id}&instanceId=${t.instanceId}` })
    }
  }
}
</script>

<style>
.row { display: flex; justify-content: space-between; align-items: center; }
.biz { font-size: 30rpx; font-weight: bold; }
.empty { text-align: center; padding: 80rpx 0; display: block; }
</style>

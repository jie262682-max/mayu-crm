<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view v-for="n in rows" :key="n.id" class="card" @click="read(n)">
      <view class="row">
        <text class="t">{{ n.title }}</text>
        <text v-if="n.readFlag === 0" class="dot">未读</text>
      </view>
      <view class="c">{{ n.content }}</view>
      <view class="muted">{{ n.createTime }}</view>
    </view>
    <view v-if="!loading && rows.length === 0" class="muted empty">暂无消息</view>
    <view v-if="finished && rows.length > 0" class="muted empty">没有更多了</view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { rows: [], pageNo: 1, loading: false, finished: false }
  },
  onLoad() {
    this.load()
  },
  onReachBottom() {
    if (!this.finished && !this.loading) this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const p = await http.get('/api/notice/page', { pageNo: this.pageNo, pageSize: 15 })
        this.rows = this.rows.concat(p.records || [])
        if (this.rows.length >= Number(p.total || 0)) this.finished = true
        else this.pageNo++
      } finally {
        this.loading = false
      }
    },
    read(n) {
      if (n.readFlag === 0) {
        http.put('/api/notice/read', { id: n.id }).then(() => { n.readFlag = 1 })
      }
    }
  }
}
</script>

<style>
.row { display: flex; justify-content: space-between; align-items: center; }
.t { font-size: 30rpx; font-weight: bold; }
.dot { font-size: 20rpx; color: #fff; background: #f56c6c; border-radius: 999rpx; padding: 2rpx 14rpx; }
.c { font-size: 26rpx; margin: 8rpx 0; color: #555; }
.empty { text-align: center; padding: 60rpx 0; display: block; }
</style>

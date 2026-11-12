<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="search">
      <input v-model="kw" class="ipt" placeholder="商机名称" confirm-type="search" @confirm="reload" />
      <button class="sbtn" size="mini" @click="reload">搜索</button>
    </view>
    <view v-for="o in rows" :key="o.id" class="card item" @click="goDetail(o)">
      <view class="row">
        <text class="name">{{ o.name }}</text>
        <text :class="['tag', o.endStatus === 1 ? 't1' : o.endStatus === 2 ? 't3' : 't0']">
          {{ o.endStatus === 1 ? '赢单' : o.endStatus === 2 ? '输单' : '进行中' }}
        </text>
      </view>
      <view class="muted">{{ o.amount || 0 }} 元 · 预计成交 {{ o.expectedDealDate || '-' }}</view>
      <view class="muted">客户ID：{{ o.customerId }}</view>
    </view>
    <view v-if="!loading && rows.length === 0" class="muted empty">暂无商机</view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { rows: [], pageNo: 1, kw: '', loading: false, finished: false }
  },
  onShow() {
    if (!uni.getStorageSync('token')) return
    if (this.rows.length === 0) this.reload()
  },
  onReachBottom() {
    if (!this.finished && !this.loading) this.load()
  },
  methods: {
    async reload() {
      this.pageNo = 1; this.finished = false; this.rows = []
      this.load()
    },
    async load() {
      this.loading = true
      try {
        const p = await http.get('/api/crm/opportunity/page', { pageNo: this.pageNo, pageSize: 15, keyword: this.kw || undefined })
        this.rows = this.rows.concat(p.records || [])
        if (this.rows.length >= Number(p.total || 0)) this.finished = true
        else this.pageNo++
      } finally {
        this.loading = false
      }
    },
    goDetail(o) {
      uni.navigateTo({ url: '/pages/opportunity/detail?id=' + o.id })
    }
  }
}
</script>

<style>
.search { display: flex; align-items: center; padding: 20rpx; gap: 16rpx; }
.ipt { flex: 1; background: #fff; border-radius: 12rpx; padding: 16rpx 24rpx; font-size: 28rpx; }
.sbtn { background: #007aff; color: #fff; border-radius: 12rpx; }
.item .row { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 30rpx; font-weight: bold; flex: 1; }
.tag { font-size: 22rpx; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; }
.t0 { color: #909399; border: 1rpx solid #909399; }
.t1 { color: #67c23a; border: 1rpx solid #67c23a; }
.t3 { color: #f56c6c; border: 1rpx solid #f56c6c; }
.empty { text-align: center; padding: 60rpx 0; display: block; }
</style>

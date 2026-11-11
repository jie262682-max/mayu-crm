<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="search">
      <input v-model="keyword" class="ipt" placeholder="搜索客户名称/手机号" confirm-type="search" @confirm="reload" />
      <button class="sbtn" size="mini" @click="reload">搜索</button>
    </view>

    <view v-for="c in rows" :key="c.id" class="card item" @click="goDetail(c.id)">
      <view class="row">
        <text class="name">{{ c.name }}</text>
        <text :class="['tag', c.dealStatus === 1 ? 'ok' : '']">{{ c.dealStatus === 1 ? '已成交' : '未成交' }}</text>
      </view>
      <view class="muted">{{ c.mobile || c.telephone || '暂无电话' }}</view>
      <view class="muted">负责人：{{ c.ownerUserId }}</view>
    </view>

    <view v-if="!loading && rows.length === 0" class="muted empty">暂无客户</view>
    <view v-if="loading" class="muted empty">加载中...</view>
    <view v-if="finished && rows.length > 0" class="muted empty">没有更多了</view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { rows: [], total: 0, pageNo: 1, keyword: '', loading: false, finished: false }
  },
  onShow() {
    if (!uni.getStorageSync('token')) return
    if (this.rows.length === 0) this.reload()
  },
  onPullDownRefresh() {
    this.reload().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (!this.finished && !this.loading) this.load()
  },
  methods: {
    async reload() {
      this.pageNo = 1; this.finished = false; this.rows = []
      await this.load()
    },
    async load() {
      this.loading = true
      try {
        const p = await http.get('/api/crm/customer/page', {
          pageNo: this.pageNo, pageSize: 15, keyword: this.keyword || undefined
        })
        this.rows = this.rows.concat(p.records || [])
        this.total = Number(p.total || 0)
        if (this.rows.length >= this.total) this.finished = true
        else this.pageNo++
      } finally {
        this.loading = false
      }
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/customer/detail?id=' + id })
    }
  }
}
</script>

<style>
.search { display: flex; align-items: center; padding: 20rpx; gap: 16rpx; }
.ipt { flex: 1; background: #fff; border-radius: 12rpx; padding: 16rpx 24rpx; font-size: 28rpx; }
.sbtn { background: #007aff; color: #fff; border-radius: 12rpx; }
.item .row { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 32rpx; font-weight: bold; }
.tag { font-size: 22rpx; color: #e6a23c; border: 1rpx solid #e6a23c; border-radius: 8rpx; padding: 2rpx 12rpx; }
.tag.ok { color: #67c23a; border-color: #67c23a; }
.empty { text-align: center; padding: 60rpx 0; display: block; }
</style>

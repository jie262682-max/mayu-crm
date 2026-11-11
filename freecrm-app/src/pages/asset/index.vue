<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="search">
      <input v-model="kw" class="ipt" placeholder="资产名称/编号" confirm-type="search" @confirm="reload" />
      <button class="sbtn" size="mini" @click="reload">搜索</button>
    </view>
    <view class="chips">
      <text v-for="(n, i) in ['全部', '闲置', '在用', '维修', '报废']" :key="i"
            :class="['chip', status === i - 1 ? 'on' : '']" @click="status = i - 1; reload()">{{ n }}</text>
    </view>

    <view v-for="a in rows" :key="a.id" class="card item" @click="goDetail(a)">
      <view class="row">
        <text class="name">{{ a.name }}</text>
        <text :class="['tag', 't' + a.status]">{{ ['闲置', '在用', '维修', '报废'][a.status] }}</text>
      </view>
      <view class="muted">{{ a.assetNo }} · {{ a.model || '-' }}</view>
      <view class="muted">保管人：{{ a.keeperUserId || '-' }} · {{ a.location || '-' }}</view>
      <view v-if="a.status === 0" class="actions">
        <button class="opbtn" size="mini" @click.stop="receive(a)">我要领用</button>
      </view>
    </view>
    <view v-if="!loading && rows.length === 0" class="muted empty">暂无资产</view>
    <view v-if="finished && rows.length > 0" class="muted empty">没有更多了</view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { rows: [], pageNo: 1, kw: '', status: null, loading: false, finished: false }
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
        const p = await http.get('/api/eam/asset/page', {
          pageNo: this.pageNo, pageSize: 15,
          keyword: this.kw || undefined,
          status: this.status === null || this.status === -1 ? undefined : this.status
        })
        this.rows = this.rows.concat(p.records || [])
        if (this.rows.length >= Number(p.total || 0)) this.finished = true
        else this.pageNo++
      } finally {
        this.loading = false
      }
    },
    goDetail(a) {
      uni.navigateTo({ url: '/pages/asset/detail?id=' + a.id })
    },
    async receive(a) {
      const ok = await new Promise(r => uni.showModal({
        title: '领用资产', content: `领用「${a.name}（${a.assetNo}）」？`,
        success: res => r(res.confirm)
      }))
      if (!ok) return
      try {
        await http.put('/api/eam/asset/receive', { id: a.id })
        uni.showToast({ icon: 'success', title: '领用成功' })
        this.reload()
      } catch (e) { /* 无权限会 toast */ }
    }
  }
}
</script>

<style>
.search { display: flex; align-items: center; padding: 20rpx; gap: 16rpx; }
.ipt { flex: 1; background: #fff; border-radius: 12rpx; padding: 16rpx 24rpx; font-size: 28rpx; }
.sbtn { background: #007aff; color: #fff; border-radius: 12rpx; }
.chips { display: flex; gap: 14rpx; padding: 0 20rpx 16rpx; flex-wrap: wrap; }
.chip { font-size: 24rpx; color: #666; border: 1rpx solid #ddd; border-radius: 999rpx; padding: 6rpx 24rpx; }
.chip.on { color: #007aff; border-color: #007aff; }
.item .row { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 30rpx; font-weight: bold; flex: 1; }
.tag { font-size: 22rpx; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; }
.t0 { color: #909399; border: 1rpx solid #909399; }
.t1 { color: #67c23a; border: 1rpx solid #67c23a; }
.t2 { color: #e6a23c; border: 1rpx solid #e6a23c; }
.t3 { color: #f56c6c; border: 1rpx solid #f56c6c; }
.actions { margin-top: 12rpx; display: flex; justify-content: flex-end; }
.opbtn { background: #007aff; color: #fff; font-size: 24rpx; border-radius: 10rpx; }
.empty { text-align: center; padding: 60rpx 0; display: block; }
</style>

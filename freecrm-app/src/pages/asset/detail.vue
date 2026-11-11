<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="card">
      <view class="row">
        <text class="h">{{ a.name || '-' }}</text>
        <text :class="['tag', 't' + a.status]">{{ ['闲置', '在用', '维修', '报废'][a.status ?? 0] }}</text>
      </view>
      <view class="kv"><text class="k">资产编号</text><text>{{ a.assetNo }}</text></view>
      <view class="kv"><text class="k">型号</text><text>{{ a.model || '-' }}</text></view>
      <view class="kv"><text class="k">序列号</text><text>{{ a.serialNo || '-' }}</text></view>
      <view class="kv"><text class="k">购置日期</text><text>{{ a.purchaseDate || '-' }}</text></view>
      <view class="kv"><text class="k">购置金额</text><text>￥{{ a.purchaseAmount || 0 }}</text></view>
      <view class="kv"><text class="k">折旧年限</text><text>{{ a.useYears || 5 }} 年</text></view>
      <view class="kv"><text class="k">保管人ID</text><text>{{ a.keeperUserId || '-' }}</text></view>
      <view class="kv"><text class="k">存放位置</text><text>{{ a.location || '-' }}</text></view>
      <view class="kv"><text class="k">备注</text><text>{{ a.remark || '-' }}</text></view>

      <view class="btns">
        <button v-if="a.status === 0" class="b primary" @click="act('/receive', '领用')">领用</button>
        <button v-if="a.status === 1" class="b warn" @click="act('/give-back', '退回')">退回</button>
        <button v-if="a.status !== 2 && a.status !== 3" class="b plain" @click="act('/repair', '报修')">报修</button>
      </view>
    </view>

    <view class="card">
      <view class="h2">变更历史</view>
      <view v-for="c in changes" :key="c.id" class="chg">
        <view class="frow">
          <text class="ctype">{{ ['', '领用', '退回', '借出', '归还', '维修', '报废'][c.type] }}</text>
          <text class="muted">{{ c.changeTime }}</text>
        </view>
        <view class="muted" v-if="c.fromUserId || c.toUserId">
          {{ c.fromUserId || '-' }} → {{ c.toUserId || '-' }} {{ c.reason || '' }}
        </view>
      </view>
      <view v-if="changes.length === 0" class="muted" style="padding:12rpx 0">暂无变更</view>
    </view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { a: {}, changes: [], id: null }
  },
  onLoad(q) {
    this.id = q.id
    this.load()
  },
  methods: {
    async load() {
      this.a = await http.get('/api/eam/asset/get', { id: this.id })
      const p = await http.get('/api/eam/asset/change/page', { pageNo: 1, pageSize: 50, assetId: this.id })
      this.changes = p.records || []
    },
    act(path, label) {
      uni.showModal({
        title: label,
        editable: true,
        placeholderText: label + '原因（可空）',
        success: async res => {
          if (!res.confirm) return
          try {
            const r = await http.put('/api/eam/asset' + path, { id: this.id, reason: res.content || undefined })
            if (path === '/scrap' && r && r.flowStarted) {
              uni.showToast({ icon: 'none', title: '已提交报废审批' })
            } else {
              uni.showToast({ icon: 'success', title: label + '成功' })
            }
            this.load()
          } catch (e) { /* toast 已提示 */ }
        }
      })
    }
  }
}
</script>

<style>
.row { display: flex; justify-content: space-between; align-items: center; }
.h { font-size: 34rpx; font-weight: bold; flex: 1; }
.h2 { font-size: 30rpx; font-weight: bold; margin-bottom: 12rpx; }
.kv { display: flex; padding: 10rpx 0; font-size: 28rpx; }
.k { width: 170rpx; color: #999; flex-shrink: 0; }
.tag { font-size: 22rpx; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; }
.t0 { color: #909399; border: 1rpx solid #909399; }
.t1 { color: #67c23a; border: 1rpx solid #67c23a; }
.t2 { color: #e6a23c; border: 1rpx solid #e6a23c; }
.t3 { color: #f56c6c; border: 1rpx solid #f56c6c; }
.btns { display: flex; gap: 16rpx; margin-top: 20rpx; }
.b { flex: 1; font-size: 28rpx; border-radius: 12rpx; margin: 0; }
.primary { background: #007aff; color: #fff; }
.warn { background: #e6a23c; color: #fff; }
.plain { background: #fff; color: #007aff; border: 1rpx solid #007aff; }
.chg { border-bottom: 1rpx solid #f0f0f0; padding: 14rpx 0; }
.frow { display: flex; justify-content: space-between; margin-bottom: 4rpx; }
.ctype { font-size: 26rpx; color: #007aff; font-weight: bold; }
</style>

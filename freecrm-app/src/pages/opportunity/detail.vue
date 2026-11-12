<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="card">
      <view class="row">
        <text class="h">{{ o.name || '-' }}</text>
        <text :class="['tag', o.endStatus === 1 ? 't1' : o.endStatus === 2 ? 't3' : 't0']">
          {{ o.endStatus === 1 ? '赢单' : o.endStatus === 2 ? '输单' : '进行中' }}
        </text>
      </view>
      <view class="kv"><text class="k">金额</text><text class="money">{{ o.amount || 0 }} 元</text></view>
      <view class="kv"><text class="k">客户ID</text><text>{{ o.customerId }}</text></view>
      <view class="kv"><text class="k">预计成交</text><text>{{ o.expectedDealDate || '-' }}</text></view>
      <view class="kv"><text class="k">备注</text><text>{{ o.remark || '-' }}</text></view>
    </view>
    <view class="card">
      <view class="h2">产品明细</view>
      <view v-for="i in items" :key="i.id" class="chg">
        <view class="frow">
          <text class="pname">{{ i.productName || '产品#' + i.productId }}</text>
          <text>{{ i.amount }} 元</text>
        </view>
        <view class="muted">{{ i.quantity }} x {{ i.price }} 元</view>
      </view>
      <view v-if="items.length === 0" class="muted" style="padding:12rpx 0">暂无明细</view>
    </view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { o: {}, items: [] }
  },
  onLoad(q) {
    this.load(q.id)
  },
  methods: {
    async load(id) {
      this.o = await http.get('/api/crm/opportunity/get', { id })
      try {
        this.items = await http.get('/api/crm/opportunity/opp-item/list', { opportunityId: id }) || []
      } catch (e) { this.items = [] }
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
.money { color: #e64340; font-weight: bold; }
.tag { font-size: 22rpx; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; }
.t0 { color: #909399; border: 1rpx solid #909399; }
.t1 { color: #67c23a; border: 1rpx solid #67c23a; }
.t3 { color: #f56c6c; border: 1rpx solid #f56c6c; }
.chg { border-bottom: 1rpx solid #f0f0f0; padding: 14rpx 0; }
.frow { display: flex; justify-content: space-between; margin-bottom: 4rpx; }
.pname { font-size: 28rpx; font-weight: bold; }
</style>

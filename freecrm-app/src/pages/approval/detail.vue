<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <!-- 流程头 -->
    <view class="card">
      <view class="row">
        <text class="h">{{ inst.bizTitle || BIZ_NAMES[bizType] || '审批' }}</text>
        <text :class="['st', 'st' + inst.status]">{{ INST_STATUS[inst.status] || '-' }}</text>
      </view>
      <view class="muted" style="margin-top:8rpx">发起人 ID：{{ inst.applicantUserId }} · 当前第 {{ inst.currentLevel }} 级</view>
    </view>

    <!-- 业务单（合同 / 回款 / 请假） -->
    <view class="card">
      <view class="h2">{{ BIZ_NAMES[bizType] || '业务单' }}</view>
      <template v-if="bizType === 1 && biz">
        <view class="kv"><text class="k">合同编号</text><text>{{ biz.contractNo }}</text></view>
        <view class="kv"><text class="k">合同名称</text><text>{{ biz.name || '-' }}</text></view>
        <view class="kv"><text class="k">客户 ID</text><text>{{ biz.customerId }}</text></view>
        <view class="kv"><text class="k">合同金额</text><text class="money">￥{{ biz.totalAmount }}</text></view>
        <view class="kv"><text class="k">已回款</text><text>￥{{ biz.receivedAmount || 0 }}</text></view>
        <view class="kv"><text class="k">下单日期</text><text>{{ biz.orderDate || '-' }}</text></view>
      </template>
      <template v-else-if="bizType === 2 && biz">
        <view class="kv"><text class="k">关联合同</text><text>{{ contract ? contract.contractNo : biz.contractId }}</text></view>
        <view class="kv"><text class="k">回款金额</text><text class="money">￥{{ biz.amount }}</text></view>
        <view class="kv"><text class="k">回款日期</text><text>{{ biz.returnTime || '-' }}</text></view>
        <view class="kv"><text class="k">方式</text><text>{{ ['-', '现金', '转账', '支票'][biz.method] || '-' }}</text></view>
        <view class="kv"><text class="k">备注</text><text>{{ biz.remark || '-' }}</text></view>
      </template>
      <template v-else-if="bizType === 100 && biz">
        <view class="kv" v-for="f in applyFields" :key="f.key"><text class="k">{{ f.label }}</text><text>{{ applyData[f.key] }}</text></view>
      </template>
      <template v-else-if="bizType === 3 && biz">
        <view class="kv"><text class="k">类型</text><text>{{ ['事假', '病假', '年假', '调休'][biz.type - 1] }}</text></view>
        <view class="kv"><text class="k">时长</text><text>{{ biz.days }} 天</text></view>
        <view class="kv"><text class="k">开始</text><text>{{ biz.startTime }}</text></view>
        <view class="kv"><text class="k">结束</text><text>{{ biz.endTime }}</text></view>
        <view class="kv"><text class="k">事由</text><text>{{ biz.reason || '-' }}</text></view>
      </template>
      <view v-else class="muted">业务单加载中...</view>
    </view>

    <!-- 审批轨迹 -->
    <view class="card">
      <view class="h2">审批轨迹</view>
      <view v-for="t in tasks" :key="t.id" class="task">
        <view class="row">
          <text class="lv">第 {{ t.nodeLevel }} 级 · 审批人 ID {{ t.approverUserId }}</text>
          <text :class="['st', 'st' + t.status]">{{ TASK_STATUS[t.status] }}</text>
        </view>
        <view v-if="t.opinion" class="muted opinion">意见：{{ t.opinion }}</view>
        <view v-if="t.auditTime" class="muted">{{ t.auditTime }}</view>
      </view>
    </view>

    <!-- 审批操作（有待办任务时） -->
    <view v-if="taskId && inst.status === 0" class="card">
      <textarea v-model="opinion" class="ta" placeholder="审批意见（可空）" />
      <view class="btns">
        <button class="b reject" @click="audit(false)">驳 回</button>
        <button class="b pass" @click="audit(true)">同 意</button>
      </view>
    </view>
  </view>
</template>

<script>
import http from '../../api/request'

const BIZ_API = {
  100: { url: '/api/oa/form/apply/get' },
  1: { url: '/api/crm/contract/get' },
  2: { url: '/api/crm/contract/payment/get' },
  3: { url: '/api/oa/leave/get' }
}

export default {
  data() {
    return {
      taskId: null, bizType: 0,
      inst: {}, biz: null, contract: null, tasks: [], opinion: '',
      BIZ_NAMES: { 1: '合同审批', 2: '回款审批', 3: '请假审批' },
      INST_STATUS: ['审批中', '已通过', '已驳回'],
      TASK_STATUS: ['待审批', '已同意', '已驳回']
    }
  },
  onLoad(q) {
    this.taskId = q.taskId || null
    this.load(q.instanceId)
  },
  methods: {
    async load(instanceId) {
      this.inst = await http.get('/api/oa/flow/instance-by-id', { instanceId })
      this.bizType = this.inst.bizType
      this.tasks = await http.get('/api/oa/flow/tasks', { instanceId })
      const api = BIZ_API[this.bizType]
      if (api) {
        this.biz = await http.get(api.url, { id: this.inst.bizId })
        // 回款补充合同编号
        if (this.bizType === 2 && this.biz && this.biz.contractId) {
          this.contract = await http.get('/api/crm/contract/get', { id: this.biz.contractId })
        }
      }
    },
    async audit(pass) {
      try {
        await http.put('/api/oa/flow/audit', { taskId: this.taskId, pass, opinion: this.opinion })
        uni.showToast({ icon: 'success', title: pass ? '已同意' : '已驳回' })
        this.taskId = null
        this.opinion = ''
        this.load(this.inst.id)
      } catch (e) { /* toast 已提示 */ }
    }
  }
}
</script>

<style>
.row { display: flex; justify-content: space-between; align-items: center; }
.h { font-size: 32rpx; font-weight: bold; flex: 1; }
.h2 { font-size: 30rpx; font-weight: bold; margin-bottom: 12rpx; }
.kv { display: flex; padding: 10rpx 0; font-size: 28rpx; }
.k { width: 170rpx; color: #999; flex-shrink: 0; }
.money { color: #e64340; font-weight: bold; }
.st { font-size: 22rpx; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; }
.st0 { color: #e6a23c; border: 1rpx solid #e6a23c; }
.st1 { color: #67c23a; border: 1rpx solid #67c23a; }
.st2 { color: #f56c6c; border: 1rpx solid #f56c6c; }
.task { border-bottom: 1rpx solid #f0f0f0; padding: 16rpx 0; }
.lv { font-size: 26rpx; }
.opinion { margin-top: 6rpx; }
.ta {
  width: 100%; box-sizing: border-box; background: #f7f8fa;
  border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; height: 120rpx;
}
.btns { display: flex; gap: 20rpx; margin-top: 20rpx; }
.b { flex: 1; font-size: 30rpx; border-radius: 12rpx; margin: 0; }
.pass { background: #007aff; color: #fff; }
.reject { background: #fff; color: #f56c6c; border: 1rpx solid #f56c6c; }
</style>

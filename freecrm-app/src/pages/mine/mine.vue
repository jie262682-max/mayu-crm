<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="card user">
      <view class="avatar">{{ (user.nickname || '?').slice(0, 1) }}</view>
      <view>
        <view class="nick">{{ user.nickname || '未登录' }}</view>
        <view class="muted">ID：{{ user.userId || '-' }}</view>
      </view>
    </view>

    <view class="card">
      <view class="kv" @click="nav('/pages/index/index')">
        <text>我的客户</text><text class="muted">›</text>
      </view>
      <view class="kv" @click="nav('/pages/todo/todo')">
        <text>待办审批</text><text class="muted">›</text>
      </view>
      <view class="kv" @click="goNotice">
        <text>我的消息</text><text class="muted">›</text>
      </view>
      <view class="kv" @click="goOpp">
        <text>我的商机</text><text class="muted">›</text>
      </view>
    </view>

    <view class="card">
      <button class="logout" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script>
import { BASE_URL } from '../../api/request'

export default {
  data() {
    return { user: uni.getStorageSync('user') || {} }
  },
  methods: {
    nav(url) {
      uni.switchTab({ url })
    },
    goNotice() {
      uni.navigateTo({ url: '/pages/notice/notice' })
    },
    goOpp() {
      uni.navigateTo({ url: '/pages/opportunity/index' })
    },
    logout() {
      uni.request({
        url: BASE_URL + '/api/auth/logout',
        method: 'POST',
        header: { Authorization: uni.getStorageSync('token') }
      })
      uni.removeStorageSync('token')
      uni.removeStorageSync('user')
      uni.reLaunch({ url: '/pages/login/login' })
    }
  }
}
</script>

<style>
.user { display: flex; align-items: center; gap: 24rpx; padding: 40rpx 24rpx; }
.avatar {
  width: 100rpx; height: 100rpx; border-radius: 50%;
  background: #007aff; color: #fff; font-size: 44rpx;
  display: flex; align-items: center; justify-content: center;
}
.nick { font-size: 34rpx; font-weight: bold; }
.kv {
  display: flex; justify-content: space-between;
  padding: 26rpx 8rpx; font-size: 30rpx;
  border-bottom: 1rpx solid #f4f4f4;
}
.logout { background: #fff; color: #e64340; font-size: 30rpx; }
</style>

<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view class="wrap">
    <view class="title">码域CRM</view>
    <view class="sub">移动办公 · 客户 / 审批</view>
    <view class="form">
      <input v-model="username" class="ipt" placeholder="用户名" placeholder-class="ph" />
      <input v-model="password" class="ipt" password placeholder="密码" placeholder-class="ph" @confirm="doLogin" />
      <button class="btn" :loading="loading" @click="doLogin">登 录</button>
    </view>
  </view>
</template>

<script>
import http from '../../api/request'

export default {
  data() {
    return { username: 'admin', password: '', loading: false }
  },
  onLoad() {
    if (uni.getStorageSync('token')) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  },
  methods: {
    async doLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ icon: 'none', title: '请输入用户名和密码' })
        return
      }
      this.loading = true
      try {
        const data = await http.post('/api/auth/login', {
          username: this.username, password: this.password
        })
        uni.setStorageSync('token', data.token)
        uni.setStorageSync('user', { userId: data.userId, nickname: data.nickname })
        uni.switchTab({ url: '/pages/index/index' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style>
.wrap { padding: 160rpx 60rpx 0; }
.title { font-size: 52rpx; font-weight: bold; text-align: center; }
.sub { color: #999; font-size: 26rpx; text-align: center; margin: 12rpx 0 80rpx; }
.ipt {
  background: #fff; border-radius: 12rpx; padding: 24rpx;
  margin-bottom: 24rpx; font-size: 30rpx;
}
.ph { color: #bbb; }
.btn {
  background: #007aff; color: #fff; border-radius: 12rpx;
  margin-top: 24rpx; font-size: 32rpx;
}
</style>

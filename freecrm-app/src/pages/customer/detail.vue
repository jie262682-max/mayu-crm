<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <view>
    <view class="card">
      <view class="h">{{ c.name || '-' }}</view>
      <view class="kv"><text class="k">电话</text><text>{{ c.mobile || c.telephone || '-' }}</text></view>
      <view class="kv"><text class="k">邮箱</text><text>{{ c.email || '-' }}</text></view>
      <view class="kv"><text class="k">网址</text><text>{{ c.website || '-' }}</text></view>
      <view class="kv"><text class="k">地区</text><text>{{ c.address || '-' }}</text></view>
      <view class="kv"><text class="k">成交状态</text><text>{{ c.dealStatus === 1 ? '已成交' : '未成交' }}</text></view>
      <view class="kv"><text class="k">下次联系</text><text>{{ c.nextContactTime || '-' }}</text></view>
      <view class="kv"><text class="k">备注</text><text>{{ c.remark || '-' }}</text></view>
    </view>

    <!-- 写跟进 -->
    <view class="card">
      <view class="h2">写跟进</view>
      <view class="types">
        <text v-for="(n, i) in TYPES" :key="i" :class="['chip', fu.type === i + 1 ? 'on' : '']"
              @click="fu.type = i + 1">{{ n }}</text>
      </view>
      <textarea v-model="fu.content" class="ta" placeholder="跟进内容（必填）" />
      <view class="attach">
        <text class="attachbtn" @click="pickFollowFiles">📎 添加附件（可多选）</text>
        <view class="alist">
          <text v-for="(f, i) in nfFiles" :key="i" class="atag" @click="nfFiles.splice(i, 1)">{{ f.name }} ×</text>
        </view>
      </view>
      <view class="rowbtn">
        <picker mode="date" :value="nextDate" @change="e => nextDate = e.detail.value">
          <view class="next">{{ nextDate ? '下次联系：' + nextDate : '选择下次联系日期（可选）' }}</view>
        </picker>
        <button class="sub" :loading="sending" @click="submitFollow">提交跟进</button>
      </view>
    </view>

    <view class="card">
      <view class="h2">客户资料</view>
      <button class="upbtn" @click="pickFiles">上传资料（可多选）</button>
      <view v-for="f in files" :key="f.id" class="fitem" @click="openFile(f)">
        <view class="fname">{{ f.fileName }}</view>
        <view class="muted">{{ fmtSize(f.fileSize) }} · {{ f.createTime }}</view>
      </view>
      <view v-if="files.length === 0" class="muted" style="padding:12rpx 0">暂无资料</view>
    </view>

    <view class="card">
      <view class="h2">跟进记录</view>
      <view v-for="f in follows" :key="f.id" class="follow">
        <view class="frow">
          <text class="ftype">{{ TYPES[f.type - 1] || '跟进' }}</text>
          <text class="muted">{{ f.nextTime ? '下次：' + f.nextTime.slice(0, 10) : '' }}</text>
        </view>
        <view class="fcontent">{{ f.content }}</view>
        <view v-if="filesOf(f.id).length" class="alist">
          <text v-for="a in filesOf(f.id)" :key="a.id" class="atag dl" @click="openFile(a)">📎 {{ a.fileName }}</text>
        </view>
        <view class="muted">{{ f.createTime }}</view>
      </view>
      <view v-if="follows.length === 0" class="muted" style="padding:20rpx 0">暂无跟进</view>
    </view>
  </view>
</template>

<script>
import http, { BASE_URL } from '../../api/request'

const TYPES = ['电话', '拜访', '微信']

export default {
  data() {
    return {
      c: {}, follows: [], TYPES, files: [],
      fu: { type: 1, content: '' }, nfFiles: [],
      nextDate: '',
      sending: false,
      custId: null
    }
  },
  onLoad(query) {
    this.custId = query.id
    this.load(query.id)
  },
  methods: {
    async load(id) {
      this.c = await http.get('/api/crm/customer/get', { id })
      const p = await http.get('/api/crm/follow-up/page', { pageNo: 1, pageSize: 20, bizType: 2, bizId: id })
      this.follows = p.records || []
      try {
        this.files = await http.get('/api/crm/customer/file/list', { customerId: id })
      } catch (e) { this.files = [] }
    },
    pickFiles() {
      // #ifdef H5
      const input = document.createElement('input')
      input.type = 'file'
      input.multiple = true
      input.onchange = async () => {
        const list = Array.from(input.files || [])
        if (!list.length) return
        uni.showLoading({ title: '上传中' })
        try {
          const fd = new FormData()
          list.forEach(f => fd.append('files', f))
          await http.post('/api/crm/customer/file/upload?customerId=' + this.custId, fd)
          uni.showToast({ icon: 'success', title: '已上传 ' + list.length + ' 个' })
          this.load(this.custId)
        } catch (e) { /* toast 已提示 */ } finally {
          uni.hideLoading()
        }
      }
      input.click()
      // #endif
      // #ifndef H5
      uni.chooseFile({
        count: 9,
        success: res => {
          const paths = res.tempFiles.map(f => f.path)
          uni.uploadFile({
            url: BASE_URL + '/api/crm/customer/file/upload?customerId=' + this.custId,
            filePath: paths[0],
            name: 'files',
            header: { Authorization: uni.getStorageSync('token') },
            success: () => { uni.showToast({ icon: 'success', title: '已上传' }); this.load(this.custId) }
          })
        }
      })
      // #endif
    },
    openFile(f) {
      const url = BASE_URL + '/api/crm/customer/file/download?id=' + f.id
      // #ifdef H5
      const token = uni.getStorageSync('token')
      fetch(url, { headers: { Authorization: token } }).then(r => r.blob()).then(b => {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(b)
        a.download = f.fileName
        a.click()
      })
      // #endif
      // #ifndef H5
      uni.downloadFile({
        url,
        header: { Authorization: uni.getStorageSync('token') },
        success: res => uni.openDocument({ filePath: res.tempFilePath, showMenu: true })
      })
      // #endif
    },
    filesOf(followId) {
      return (this.files || []).filter(x => x.followUpId === followId)
    },
    pickFollowFiles() {
      // #ifdef H5
      const input = document.createElement('input')
      input.type = 'file'
      input.multiple = true
      input.onchange = () => { this.nfFiles.push(...Array.from(input.files || [])) }
      input.click()
      // #endif
      // #ifndef H5
      uni.chooseFile({
        count: 9,
        success: res => { this.nfFiles.push(...res.tempFiles.map(f => ({ name: f.name, path: f.path }))) }
      })
      // #endif
    },
    async uploadFollowFiles(followUpId) {
      // #ifdef H5
      if (this.nfFiles.length) {
        const fd = new FormData()
        this.nfFiles.forEach(f => fd.append('files', f))
        await http.post('/api/crm/customer/file/upload?customerId=' + this.custId + '&followUpId=' + followUpId, fd)
      }
      // #endif
      // #ifndef H5
      for (const f of this.nfFiles) {
        await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: BASE_URL + '/api/crm/customer/file/upload?customerId=' + this.custId + '&followUpId=' + followUpId,
            filePath: f.path,
            name: 'files',
            header: { Authorization: uni.getStorageSync('token') },
            success: resolve,
            fail: reject
          })
        })
      }
      // #endif
      this.nfFiles = []
    },
    fmtSize(n) {
      if (!n) return '-'
      return n > 1048576 ? (n / 1048576).toFixed(1) + 'MB' : (n / 1024).toFixed(0) + 'KB'
    },
    async submitFollow() {
      if (!this.fu.content || !this.fu.content.trim()) {
        uni.showToast({ icon: 'none', title: '请填写跟进内容' })
        return
      }
      this.sending = true
      try {
        const followId = await http.post('/api/crm/follow-up/save', {
          bizType: 2, bizId: this.custId, type: this.fu.type,
          content: this.fu.content.trim(),
          nextTime: this.nextDate ? this.nextDate + ' 10:00:00' : null
        })
        try {
          await this.uploadFollowFiles(followId)
        } catch (e) { /* 附件失败不吞跟进结果 */ }
        uni.showToast({ icon: 'success', title: '跟进已记录' })
        this.fu.content = ''
        this.nextDate = ''
        this.load(this.custId)
      } catch (e) { /* toast 已提示（如无写权限 403） */ } finally {
        this.sending = false
      }
    }
  }
}
</script>

<style>
.h { font-size: 36rpx; font-weight: bold; margin-bottom: 16rpx; }
.h2 { font-size: 30rpx; font-weight: bold; margin-bottom: 12rpx; }
.kv { display: flex; padding: 10rpx 0; font-size: 28rpx; }
.k { width: 160rpx; color: #999; flex-shrink: 0; }
.types { display: flex; gap: 16rpx; margin-bottom: 16rpx; }
.chip {
  font-size: 26rpx; color: #666; border: 1rpx solid #ddd;
  border-radius: 999rpx; padding: 6rpx 28rpx;
}
.chip.on { color: #007aff; border-color: #007aff; }
.ta {
  width: 100%; box-sizing: border-box; background: #f7f8fa;
  border-radius: 12rpx; padding: 20rpx; font-size: 28rpx; height: 140rpx;
}
.rowbtn { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.next { font-size: 26rpx; color: #007aff; }
.sub { background: #007aff; color: #fff; font-size: 28rpx; border-radius: 12rpx; margin: 0; }
.follow { border-bottom: 1rpx solid #f0f0f0; padding: 16rpx 0; }
.frow { display: flex; justify-content: space-between; margin-bottom: 6rpx; }
.ftype { font-size: 22rpx; color: #007aff; border: 1rpx solid #007aff; border-radius: 8rpx; padding: 0 12rpx; }
.fcontent { font-size: 28rpx; margin-bottom: 6rpx; }
.attach { margin-top: 14rpx; }
.attachbtn { font-size: 26rpx; color: #007aff; }
.alist { display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 10rpx; }
.atag { font-size: 22rpx; color: #555; background: #f2f3f5; border-radius: 8rpx; padding: 4rpx 14rpx; }
.atag.dl { color: #007aff; background: #eef5ff; }
.upbtn { background: #eef5ff; color: #007aff; font-size: 26rpx; border-radius: 12rpx; margin-bottom: 12rpx; }
.fitem { padding: 14rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.fname { font-size: 28rpx; color: #333; margin-bottom: 4rpx; }
</style>

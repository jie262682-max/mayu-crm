<!--
  码域CRM（MaoyuCRM）Copyright (c) 2026 合肥市码域科技
  AGPL-3.0 许可，详见仓库 LICENSE
-->
<template>
  <el-card>
    <div style="margin-bottom:12px;display:flex;gap:8px;align-items:center">
      <el-radio-group v-model="onlyUnread" @change="load">
        <el-radio-button :value="false">全部</el-radio-button>
        <el-radio-button :value="true">未读</el-radio-button>
      </el-radio-group>
      <el-button plain @click="readAll">全部已读</el-button>
    </div>
    <el-table :data="rows" border stripe @row-click="onRow">
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-badge is-dot :hidden="row.readFlag === 1"><span style="padding-left:6px"></span></el-badge>
          <el-tag v-if="row.readFlag === 1" type="info" size="small">已读</el-tag>
          <el-tag v-else size="small">未读</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="['', 'warning', 'success', 'info'][row.type]" size="small">
            {{ ['', '审批待办', '审批结果', '系统'][row.type] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="200" />
      <el-table-column prop="content" label="内容" min-width="240" />
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.type === 1 && row.bizType" link type="primary" @click.stop="goBiz(row)">去处理</el-button>
          <el-button v-if="row.readFlag === 0" link @click.stop="read(row)">已读</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" v-model:current-page="pageNo"
                   :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="load" />
  </el-card>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../../api/request'

const router = useRouter()
const rows = ref([]); const total = ref(0); const pageNo = ref(1); const onlyUnread = ref(false)

async function load() {
  const p = await http.get('/api/notice/page', { params: { pageNo: pageNo.value, pageSize: 10, unreadOnly: onlyUnread.value || undefined } })
  rows.value = p.records; total.value = Number(p.total)
}
function read(row) {
  http.put('/api/notice/read', null, { params: { id: row.id } }).then(load)
}
function readAll() {
  http.put('/api/notice/read', null, { params: {} }).then(load)
}
function onRow(row) {
  if (row.readFlag === 0) read(row)
}
function goBiz(row) {
  read(row)
  if (row.bizType === 3) {
    router.push('/oa/todo')
  } else {
    router.push('/crm/contract')
  }
}
onMounted(load)
</script>

<template>
  <h1 class="page-title">令牌信息</h1>

  <div class="card">
    <h2>当前会话</h2>
    <dl class="kv">
      <dt>用户（sub）</dt>
      <dd>{{ auth.subject }}</dd>
      <dt>权限组</dt>
      <dd>
        <span v-for="g in auth.groups" :key="g" class="badge" :class="{ success: g === 'ADMIN' }">
          {{ g === 'ADMIN' ? '管理员' : g === 'USER' ? '普通用户' : g }}
        </span>
        <span v-if="!auth.groups.length" class="muted">（无）</span>
      </dd>
      <dt>授权范围</dt>
      <dd>
        <span v-for="s in auth.scopes" :key="s" class="badge">{{ s }}</span>
      </dd>
      <dt>签发者（iss）</dt>
      <dd>{{ auth.claims?.iss }}</dd>
      <dt>过期时间</dt>
      <dd>
        {{ auth.expiresAt?.toLocaleString() }}
        <span class="badge" :class="auth.isExpired ? 'danger' : 'success'">
          {{ auth.isExpired ? '已过期' : '有效' }}
        </span>
      </dd>
    </dl>
  </div>

  <div class="card">
    <h2>Access Token（JWT）</h2>
    <pre>{{ auth.tokens?.access_token }}</pre>
  </div>

  <div class="card">
    <h2>操作</h2>
    <div class="actions">
      <button v-if="auth.tokens?.refresh_token" class="btn btn-outline" :disabled="refreshing" @click="onRefresh">
        {{ refreshing ? '刷新中…' : '用 refresh_token 换新令牌' }}
      </button>
      <RouterLink to="/api-test" class="btn btn-primary">去测试 API</RouterLink>
    </div>
    <div v-if="message" class="alert alert-error">{{ message }}</div>
    <p class="muted">
      公共客户端按 OAuth 2.1 安全实践不签发 refresh_token，令牌过期后需重新登录；
      refresh_token 流程可用机密客户端 demo-client 通过 curl 演示（见 README）。
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const refreshing = ref(false)
const message = ref('')

async function onRefresh() {
  refreshing.value = true
  message.value = ''
  try {
    await auth.refresh()
  } catch (e) {
    message.value = e.message
  } finally {
    refreshing.value = false
  }
}
</script>

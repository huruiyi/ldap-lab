<template>
  <h1 class="page-title">API 测试</h1>

  <div class="card">
    <h2>受保护资源</h2>
    <div class="actions">
      <button class="btn btn-primary" :disabled="busy" @click="callHello">GET /api/hello</button>
      <button class="btn btn-outline" :disabled="busy" @click="callEcho">POST /api/echo</button>
      <button class="btn btn-outline" :disabled="busy" @click="callAdmin">GET /api/admin/stats</button>
      <button class="btn btn-danger" :disabled="busy" @click="callWithoutToken">不带 token 调用</button>
    </div>
    <p class="muted" style="margin-bottom: 12px">
      /api/hello 需要 <b>api.read</b> scope；/api/echo 需要 <b>api.write</b> scope；
      /api/admin/stats 需要 <b>管理员组</b>（demo 用户应 403，admin 用户 200）；不带 token 应返回 401。
    </p>
    <template v-if="result">
      <p style="margin-bottom: 8px">
        <span class="badge" :class="result.status < 400 ? 'success' : 'danger'">HTTP {{ result.status }}</span>
        <span class="muted">{{ result.label }}</span>
      </p>
      <pre>{{ pretty(result.data) }}</pre>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const busy = ref(false)
const result = ref(null)

function pretty(data) {
  return typeof data === 'string' ? data : JSON.stringify(data, null, 2)
}

async function run(label, fn) {
  busy.value = true
  try {
    const r = await fn()
    result.value = { label, ...r }
  } catch (e) {
    result.value = { label, status: 'ERR', data: e.message }
  } finally {
    busy.value = false
  }
}

function callHello() {
  run('GET /api/hello', () => auth.apiFetch('/api/hello'))
}

function callEcho() {
  run('POST /api/echo', () =>
    auth.apiFetch('/api/echo', {
      method: 'POST',
      body: JSON.stringify({ msg: '你好，OAuth2', time: new Date().toISOString() })
    })
  )
}

function callAdmin() {
  run('GET /api/admin/stats（需管理员组）', () => auth.apiFetch('/api/admin/stats'))
}

async function callWithoutToken() {
  run('GET /api/hello（无 token）', async () => {
    const response = await fetch(`${auth.apiBase}/api/hello`)
    return { status: response.status, data: response.statusText || '（空响应体）' }
  })
}
</script>

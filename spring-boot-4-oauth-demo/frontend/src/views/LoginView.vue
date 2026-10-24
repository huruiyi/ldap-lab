<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-logo">
        <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      </div>
      <h1>OAuth2 Demo</h1>
      <p class="auth-subtitle">客户端应用</p>

      <div v-if="error" class="alert alert-error" style="text-align: left">{{ error }}</div>
      <div v-if="loggedOut" class="alert alert-info" style="text-align: left">✓ 已完全退出（含授权服务器会话）</div>

      <button class="btn btn-gradient" :disabled="loading" @click="onLogin">
        {{ loading ? '正在跳转授权服务器…' : '使用 OAuth2 登录' }}
      </button>

      <div class="auth-hint">
        将跳转到授权服务器统一登录页<br />
        公共客户端 <code>vue-client</code> · 无密钥 · S256 挑战码
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const loading = ref(false)
const error = ref(route.query.error || '')
const loggedOut = ref(route.query.logout !== undefined)

// strip ?logout / ?error from the address bar
if (route.query.logout !== undefined || route.query.error) {
  window.history.replaceState({}, '', '/login')
}

async function onLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login()
  } catch (e) {
    error.value = e.message
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-logo">
        <svg v-if="!error" class="auth-spinner" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round">
          <path d="M21 12a9 9 0 1 1-6.2-8.56" />
        </svg>
        <span v-else>⚠️</span>
      </div>
      <h1>{{ error ? '登录失败' : '正在完成登录…' }}</h1>
      <p class="auth-subtitle">{{ error ? '授权过程中出现问题' : '已收到授权码，正在换取令牌' }}</p>

      <div v-if="error" class="alert alert-error" style="text-align: left">{{ error }}</div>
      <RouterLink v-if="error" to="/login" class="btn btn-gradient">返回登录</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const error = ref('')

onMounted(async () => {
  if (route.query.error) {
    error.value = `${route.query.error}: ${route.query.error_description || '授权被拒绝'}`
    return
  }
  const code = route.query.code
  if (!code) {
    error.value = '回调中没有授权码'
    return
  }
  try {
    await auth.handleCallback(code)
    router.replace({ name: 'home' })
  } catch (e) {
    error.value = e.message
  }
})
</script>

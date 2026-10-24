<template>
  <template v-if="auth.isAuthenticated">
    <nav class="navbar">
      <span class="brand">🔐 OAuth2 Demo</span>
      <div class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/api-test">API 测试</RouterLink>
        <RouterLink v-if="auth.groups.includes('ADMIN')" to="/groups">权限组管理</RouterLink>
        <RouterLink v-if="auth.groups.includes('ADMIN')" to="/users">用户管理</RouterLink>
      </div>
      <div class="user-area">
        <span>{{ auth.subject }}</span>
        <button class="btn btn-danger" @click="onLogout">退出</button>
      </div>
    </nav>
    <main class="page">
      <RouterView />
    </main>
  </template>
  <RouterView v-else />
</template>

<script setup>
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()

function onLogout() {
  // navigates away through the authorization server's /logout endpoint,
  // which redirects back to the login page afterwards
  auth.logout()
}
</script>

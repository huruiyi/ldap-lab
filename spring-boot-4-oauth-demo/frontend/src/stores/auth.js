import { defineStore } from 'pinia'
import { generateVerifier, challengeFrom } from '../utils/pkce'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:9000'
const CLIENT_ID = 'vue-client'
const VERIFIER_KEY = 'pkce_verifier'
const TOKEN_KEY = 'oauth_tokens'

function loadTokens() {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_KEY)) || null
  } catch {
    return null
  }
}

function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tokens: loadTokens(),
    apiBase: API_BASE
  }),

  getters: {
    isAuthenticated: (state) => !!state.tokens?.access_token,
    claims: (state) => (state.tokens?.access_token ? decodeJwt(state.tokens.access_token) : null),
    subject() {
      return this.claims?.sub || ''
    },
    scopes() {
      return this.claims?.scope || []
    },
    groups() {
      return this.claims?.groups || []
    },
    expiresAt() {
      return this.claims?.exp ? new Date(this.claims.exp * 1000) : null
    },
    isExpired() {
      return this.expiresAt ? this.expiresAt.getTime() <= Date.now() : true
    }
  },

  actions: {
    /** Step 1 of authorization_code: bounce the browser to the authorization server. */
    async login() {
      const verifier = generateVerifier()
      sessionStorage.setItem(VERIFIER_KEY, verifier)
      const challenge = await challengeFrom(verifier)
      const redirectUri = `${window.location.origin}/callback`
      const params = new URLSearchParams({
        response_type: 'code',
        client_id: CLIENT_ID,
        redirect_uri: redirectUri,
        scope: 'api.read api.write',
        code_challenge: challenge,
        code_challenge_method: 'S256'
      })
      window.location.assign(`${API_BASE}/oauth2/authorize?${params}`)
    },

    /** Step 2: exchange the code (+ verifier proof) for tokens. */
    async handleCallback(code) {
      const verifier = sessionStorage.getItem(VERIFIER_KEY)
      if (!verifier) {
        throw new Error('PKCE verifier 已丢失，请重新登录')
      }
      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        redirect_uri: `${window.location.origin}/callback`,
        code_verifier: verifier,
        client_id: CLIENT_ID
      })
      const response = await fetch(`${API_BASE}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      })
      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.error_description || data.error || '换令牌失败')
      }
      sessionStorage.removeItem(VERIFIER_KEY)
      this.saveTokens(data)
    },

    /** Trade the refresh token for a fresh access token. */
    async refresh() {
      if (!this.tokens?.refresh_token) {
        throw new Error('没有 refresh_token')
      }
      const body = new URLSearchParams({
        grant_type: 'refresh_token',
        refresh_token: this.tokens.refresh_token,
        client_id: CLIENT_ID
      })
      const response = await fetch(`${API_BASE}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      })
      const data = await response.json()
      if (!response.ok) {
        this.logout({ sso: false })
        throw new Error(data.error_description || data.error || '刷新令牌失败')
      }
      this.saveTokens({ ...this.tokens, ...data })
    },

    /** fetch wrapper that attaches the bearer token and refreshes when expired. */
    async apiFetch(path, options = {}) {
      if (this.isExpired) {
        // SAS does not issue refresh tokens to public clients (NONE auth
        // method) — an expired token means the user must log in again
        if (!this.tokens?.refresh_token) {
          this.logout({ sso: false })
          throw new Error('登录已过期，请重新登录')
        }
        await this.refresh()
      }
      const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...(options.headers || {}),
          Authorization: `Bearer ${this.tokens.access_token}`
        }
      })
      const text = await response.text()
      let data = null
      try {
        data = text ? JSON.parse(text) : null
      } catch {
        data = text
      }
      return { status: response.status, data }
    },

    saveTokens(data) {
      this.tokens = data
      localStorage.setItem(TOKEN_KEY, JSON.stringify(data))
    },

    /**
     * Log out. With sso=true (default) the browser is sent through the
     * authorization server's /logout endpoint as well, so the SSO session
     * there is killed too — otherwise the next login would silently succeed
     * without asking for credentials.
     */
    logout({ sso = true } = {}) {
      this.tokens = null
      localStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(VERIFIER_KEY)
      if (sso) {
        const back = `${window.location.origin}/login?logout`
        window.location.assign(`${API_BASE}/logout?redirect=${encodeURIComponent(back)}`)
      }
    }
  }
})

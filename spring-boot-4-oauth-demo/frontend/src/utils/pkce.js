// PKCE (RFC 7636) helpers — the SPA is a public client, so the authorization
// code flow must carry a code_challenge and later prove it with the verifier.

const VERIFIER_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'

export function generateVerifier(length = 64) {
  const random = crypto.getRandomValues(new Uint8Array(length))
  return Array.from(random, (b) => VERIFIER_CHARS[b % VERIFIER_CHARS.length]).join('')
}

export async function challengeFrom(verifier) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  return base64UrlEncode(new Uint8Array(digest))
}

function base64UrlEncode(bytes) {
  let binary = ''
  for (const b of bytes) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

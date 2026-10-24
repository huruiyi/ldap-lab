/** 公共工具：token 存取、API 封装、消息提示 */
const TokenStore = {
  KEY: 'jwt_token',
  UID: 'jwt_uid',
  get() { return localStorage.getItem(this.KEY); },
  getUid() { return localStorage.getItem(this.UID); },
  save(uid, token) {
    localStorage.setItem(this.KEY, token);
    localStorage.setItem(this.UID, uid);
  },
  clear() {
    localStorage.removeItem(this.KEY);
    localStorage.removeItem(this.UID);
  }
};

/** 解析 JWT payload（不校验签名，仅用于页面展示） */
function parseJwt(token) {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(decodeURIComponent(escape(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))));
  } catch (e) {
    return null;
  }
}

function isLoggedIn() {
  const token = TokenStore.get();
  if (!token) return false;
  const payload = parseJwt(token);
  if (!payload || !payload.exp) return false;
  if (payload.exp * 1000 < Date.now()) {
    TokenStore.clear();
    return false;
  }
  return true;
}

function currentRoles() {
  const payload = parseJwt(TokenStore.get() || '');
  return (payload && payload.roles) || [];
}

/** 统一 API 请求，自动附带 Authorization 头；401 时跳回登录页 */
async function api(path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const token = TokenStore.get();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const resp = await fetch(path, Object.assign({}, options, { headers }));
  if (resp.status === 401 && token) {
    TokenStore.clear();
    window.location.href = '/login.html';
    throw new Error('登录已过期');
  }
  return resp;
}

function showMessage(el, text, type) {
  el.textContent = text;
  el.className = 'message ' + type;
}

function hideMessage(el) {
  el.className = 'message';
}

function logout() {
  TokenStore.clear();
  window.location.href = '/login.html';
}

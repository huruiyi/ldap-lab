document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('message');
  const btn = document.getElementById('submitBtn');
  hideMessage(msg);

  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;

  btn.disabled = true;
  btn.textContent = '登录中…';
  try {
    const resp = await fetch('/api/versions/1/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (resp.ok) {
      const data = await resp.json();
      TokenStore.save(data.uid, data.accessToken);
      window.location.href = '/index.html';
    } else if (resp.status === 401) {
      showMessage(msg, '用户名或密码错误', 'error');
    } else {
      showMessage(msg, '登录失败，请稍后重试（HTTP ' + resp.status + '）', 'error');
    }
  } catch (err) {
    showMessage(msg, '网络错误，无法连接服务器', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '登 录';
  }
});

// 已登录用户直接进首页
if (isLoggedIn()) {
  window.location.href = '/index.html';
}

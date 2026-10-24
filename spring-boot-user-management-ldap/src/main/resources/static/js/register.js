document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('message');
  const btn = document.getElementById('submitBtn');
  hideMessage(msg);

  const uid = document.getElementById('uid').value.trim();
  const fullName = document.getElementById('fullName').value.trim();
  const lastName = document.getElementById('lastName').value.trim();
  const password = document.getElementById('password').value;
  const confirmPassword = document.getElementById('confirmPassword').value;

  if (password !== confirmPassword) {
    showMessage(msg, '两次输入的密码不一致', 'error');
    return;
  }
  if (password.length < 6) {
    showMessage(msg, '密码长度至少 6 位', 'error');
    return;
  }

  btn.disabled = true;
  btn.textContent = '注册中…';
  try {
    const resp = await fetch('/api/versions/1/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ uid, fullName, lastName, password })
    });
    if (resp.status === 201) {
      showMessage(msg, '注册成功！即将跳转到登录页…', 'success');
      setTimeout(() => { window.location.href = '/login.html'; }, 1200);
    } else if (resp.status === 409) {
      showMessage(msg, '用户名已被占用，请换一个', 'error');
    } else {
      showMessage(msg, '注册失败（HTTP ' + resp.status + '）', 'error');
    }
  } catch (err) {
    showMessage(msg, '网络错误，无法连接服务器', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '注 册';
  }
});

if (isLoggedIn()) {
  window.location.href = '/index.html';
}
